/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.impl.workers.MessageDistributionException;
import com.zfabrik.impl.workers.WorkerConstants;
import com.zfabrik.impl.workers.home.io.WorkerNotifier;
import com.zfabrik.resources.IResourceManager;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.sync.SynchronizationRunner;
import com.zfabrik.work.ApplicationThreadPool;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * The home layout knows all currently running workers. It knows how to broadcast 
 * messages and how to re-establish a basic home layout (i.e. without those dynamically started workers). The Homelayout is the home process's perspective
 * on the whole worker situation
 */
public class HomeLayoutResource extends Resource {
	private String name;
	private long invalidationCount;

	private HomeLayoutImpl instance;

	public HomeLayoutResource() {}

	@Override
	public void init() {
		this.name = handle().getResourceInfo().getName();
	}
	
	public synchronized <T> T as(Class<T> clz) {
		if (IHomeLayout.class.equals(clz)) {
			if (instance==null) {
				instance = new HomeLayoutImpl();
			}
			return clz.cast(instance);
		}
		return null;
	}

	@Override
	public synchronized void invalidate() {
		this.invalidationCount++;
		try {
			if (this.instance!=null) {
				this.instance.shutdown();
			}
		} finally {
			instance=null;
		}
	}
	
	private synchronized long getInvalidationCount() {
		return this.invalidationCount;
	}

	
	
	/**
	 * The actual home layout impl
	 */
	private class HomeLayoutImpl implements IHomeLayout, HomeLayoutMBean {
		private ObjectName on;

		public HomeLayoutImpl() {
			try {
				this.on = ObjectName.getInstance("org.z2env:type="+HomeLayoutResource.class.getName());
				MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
				if (mbs.isRegistered(this.on)) {
					mbs.unregisterMBean(this.on);
				} 
				mbs.registerMBean(new StandardMBean(this, HomeLayoutMBean.class),this.on);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to register MBean for home layout: "+name,e);
			}
			
		}
		
		public void shutdown() {
			if (this.on!=null) {
				try {
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
				} catch (Exception e) {
					logger.log(Level.WARNING,"Problem unregistering home layout MBean: "+name);
				} finally {
					this.on=null;
				}
			}
		}
		
		/**
		 * Communication methods
		 */
		public void broadcastInvalidations(Collection<String> invs, long timeout, int scope, String senderWorker) throws IOException {
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("processing "+invs.size()+" invalidations. Scope is "+scope);
			}
			long c = getInvalidationCount();
			if (scope!=0) {
				SynchronizationRunner r=null;
				try {
					if ((scope & SCOPE_HOME)!=0) {
						// acquire sync runner here... down there our class loader might already reject giving it to us.
						r = new SynchronizationRunner(SynchronizationRunner.VERIFY_ONLY);
						// 2. process local invalidations
						IResourceManager.INSTANCE.invalidate(invs);
					}		
					// 3. unless we have been invalidated (which implies that all workers have been stopped), we broadcast to them
					if ((scope & SCOPE_WORKERS)!=0 && c==getInvalidationCount()) {
						Map<String,Serializable> args = new HashMap<String, Serializable>();
						args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_INVALIDATE);
						args.put(WorkerConstants.INVALIDATIONSET, (Serializable)invs);
						try {
							_broadcastMessage(args,timeout, ((scope & SCOPE_OTHERS)!=0? senderWorker:null),false);
						} catch (IOException e) {
							throw new IllegalStateException("Error during send of invalidation message to worker processes",e);
						}
					}
				} finally {
					// schedule  a verification
					if ((scope & SCOPE_HOME)!=0) {
						r.execute(false);
					}
				}
			}
		}

		private void _broadcastMessage(Map<String, Serializable> args, long timeout, String exclude,boolean killOnTimeOut) throws IOException {
			synchronized (this) {
				// get all actually running worker processes
				Set<IWorkerProcess> ws = WorkerProcess.allWorkers();
				if (exclude!=null) {
					Iterator<IWorkerProcess> it = ws.iterator();
					while (it.hasNext()) { if (exclude.equals(it.next().getComponentName())) it.remove(); }
				}
				List<WorkerNotifier> t = new ArrayList<WorkerNotifier>(ws.size());
				for (IWorkerProcess wp : ws) {
					t.add(new WorkerNotifier(args, wp, timeout,killOnTimeOut));
				}
				ApplicationThreadPool.instance().execute(true,t);
				// collect errors
				Map<String, Throwable> c = new HashMap<String, Throwable>();
				for (WorkerNotifier n : t) {
					if (n.getError() != null) {
						c.put(n.getId(), n.getError());
					}
				}
				if (c.size() > 0)
					throw new MessageDistributionException("Error during message distribution", c);
			}
		}

		public void broadcastMessage(Map<String, Serializable> args, long timeout,boolean killOnTimeOut) throws IOException {
			this._broadcastMessage(args, timeout, null, killOnTimeOut);
		}
		
		public void broadcastMessage(Map<String, Serializable> args, long timeout) throws IOException {
			this._broadcastMessage(args, timeout, null, false);
		}
		
		@Override
		public void prepare(String componentName) throws Exception {
			Optional.ofNullable(
				IComponentsLookup.INSTANCE.lookup(componentName, IDependencyComponent.class)
			)
			.orElseThrow(()->
				new IllegalArgumentException("Component "+componentName+" does not implement "+IDependencyComponent.class.getSimpleName())
			)
			.prepare();
		}
	}
	

	private final static Logger logger = Logger.getLogger(HomeLayoutResource.class.getName());
}
