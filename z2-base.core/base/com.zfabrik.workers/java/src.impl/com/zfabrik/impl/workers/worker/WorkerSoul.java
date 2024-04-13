/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.worker;


import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_ATTAINOP;
import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_DETACH;
import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_GETLEASE;
import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_INVALIDATE;
import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_PING;
import static com.zfabrik.impl.workers.WorkerConstants.COMMAND_TERM;
import static com.zfabrik.impl.workers.WorkerConstants.INVALIDATIONSET;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.impl.workers.MessageException;
import com.zfabrik.impl.workers.MessageHandlerResource;
import com.zfabrik.impl.workers.StreamReader;
import com.zfabrik.impl.workers.home.HomeMessageHandler;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.IResourceManager;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.work.ApplicationThreadPool;
import com.zfabrik.work.WorkUnit;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IWorkerProcess;
import com.zfabrik.workers.home.WorkerUtils;
import com.zfabrik.workers.worker.HomeHandle;
import com.zfabrik.workers.worker.IWorkerLease;

/**
 * This component implementation is the life keeper of the worker processs.
 *
 * When this component is invalidated, the worker process terminates.
 * Invalidation messages are processed here.
 *
 * @author hb
 *
 */

public class WorkerSoul extends Resource implements Runnable, IMessageHandler, IWorkerLease {
	private final static String STATE_WORKER_UP = "com.zfabrik.boot.main/worker_up";

	private boolean started  = false;
	private boolean stopping = false;
	private String workerName, workerComponent;
	private String name;
	private List<IResourceHandle> deps = new LinkedList<IResourceHandle>();
	private WorkerEndPoint wep;
	@SuppressWarnings("unused")
	private HomeHandleImpl hh;
	private String pid;

	private int lease;
	private boolean detached;

	// ----------------------------

	public synchronized void decreaseLease() {
		this.lease--;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Decreased lease count of worker ("+this.pid+") to "+this.lease+": "+this.workerName);
		}
		this.checkLease();
	};

	public synchronized void increaseLease() {
		this.lease++;
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Increased lease count of worker ("+this.pid+") to "+this.lease+": "+this.workerName);
		}
	}

	private synchronized void checkLease() {
		if (this.detached && this.lease<=0) {
			logger.info("Detached worker processe's lease has expired: "+this.workerName);
			this.stop();
		}
	}

	private synchronized void detach() {
		this.detached=true;
		logger.info("Detached worker process ("+this.pid+") instance of "+this.name);
		this.checkLease();
	}

	// ----------------------------
	private class HomeHandleImpl extends HomeHandle {
		private final static long VERI_TO = 10000;
		private final static long SYNC_TO = 60000;
		private final static long GETLOG_TO    = 30000;
		private final static long BROADCAST_INV_TO = 1000;
		private long to_msg;


		public HomeHandleImpl() {
			setInstance(this);
			// read default message timeout
			try {
				this.to_msg = Long.parseLong(WorkerSoul.this.workerDesc().getProperties().getProperty(IWorkerProcess.TO_MSG, IWorkerProcess.TO_MSG_DEF));
			} catch (Exception e) {
				throw new IllegalStateException("Failed to read process communication timeout (" + IWorkerProcess.TO_MSG + ")",e);
			}

		}
		public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException {
			if (timeout<0) {
				timeout=this.to_msg;
			}
			WorkerEndPoint we;
			synchronized (WorkerSoul.this) {
				we = wep;
			}
			if (we!=null) {
				return we.sendMessage(args, timeout);
			} else {
				throw new IllegalStateException("Cannot send message to home this early!");
			}
		}

		@SuppressWarnings("unchecked")
		public List<String> getLastSynchronizationLog() throws IOException {
			Map<String,Serializable> args = _prepArgs();
			args.put(IMessageHandler.COMMAND, HomeMessageHandler.CMD_GETLOG);
			Map<String, Serializable> res = sendMessage(args, GETLOG_TO);
			return (List<String>) res.get(HomeMessageHandler.RES_LOG);
		}

		public void triggerSynchronization(int scope) throws IOException {
			Map<String, Serializable> args = _prepArgs();
			args.put(IMessageHandler.COMMAND, HomeMessageHandler.CMD_SYNC);
			args.put(HomeMessageHandler.PARAM_SCOPE, scope);
			sendMessage(args, SYNC_TO);
		}

		public void triggerVerification() throws IOException {
			Map<String,Serializable> args = _prepArgs();
			args.put(IMessageHandler.COMMAND, HomeMessageHandler.CMD_VERIFY);
			sendMessage(args, VERI_TO);
		}

		public void broadcastInvalidations(Collection<String> invs, int scope)  {
			try {
				Map<String,Serializable> args = _prepArgs();
				args.put(HomeMessageHandler.PARAM_RESOURCES, (Serializable) invs);
				args.put(HomeMessageHandler.PARAM_SCOPE, scope);
				args.put(IMessageHandler.COMMAND, HomeMessageHandler.CMD_INVALIDATION);
				sendMessage(args, BROADCAST_INV_TO);
			} catch (IOException e) {
				throw new RuntimeException("Failed to send invalivations to home process",e);
			}
		}

		//
		private Map<String, Serializable> _prepArgs() {
			Map<String,Serializable> args = new HashMap<String, Serializable>();
			args.put(IWorkerProcess.MSG_TARGET, IWorkerProcess.MSG_TARGET_HOME);
			args.put(IMessageHandler.COMMAND_GROUP, HomeMessageHandler.CMDGROUP);
			args.put(HomeMessageHandler.PARAM_WORKER,Foundation.getProperties().getProperty(Foundation.PROCESS_WORKER));
			return args;
		}

		@Override
		public IWorkerLease getWorkerLease() {
			return WorkerSoul.this;
		}
	}
	//----------------------------

	public <T> T as(Class<T> clz) {
		if (Runnable.class.equals(clz)) {
			if (this.workerName==null) {
				// initialize
				this.workerName = Foundation.getProperties().getProperty(Foundation.PROCESS_WORKER);
				if (this.workerName==null) {
					throw new IllegalStateException("Failed to retrieve worker name from foundation properties ("+Foundation.PROCESS_WORKER+"): "+name);
				}
				this.workerComponent = WorkerUtils.getWorkerComponentName(this.workerName);
				this.hh=new HomeHandleImpl();
			}
			return clz.cast(this);
		}
		return null;
	}

	public IComponentDescriptor workerDesc() {
		IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(this.workerComponent);
		if (d==null) {
			throw new IllegalStateException("Failed to retrieve worker description ("+this.workerComponent+"): "+name);
		}
		return d;
	}

	public void init() {
		this.name = handle().getResourceInfo().getName();
	}

	@Override
	public void invalidate()
			throws ResourceBusyException {
		stop();
	}

	private synchronized void stop() {
		if (!stopping && started) {
			this.notify();
		}
		stopping=true;
	}

	//
	// this is the main method of a worker process.
	// It is called from the WorkerLauncher and determines the life-time of the
	// worker process, as the worker will shut down once this method finishes.
	//
	public void run() {
		this.getPid();
		// run this early to get the right repos registered early
		this.loadWorkerUp();
		String maxConcurrency = this.workerDesc().getProperties().getProperty(IWorkerProcess.WORKER_CONCURRENCY,"5");
		try {
			int mc = Integer.parseInt(maxConcurrency);
			ApplicationThreadPool.instance().setMaxConcurrency(mc);
		} catch (NumberFormatException nfe) {
			throw new IllegalStateException("Failed to initialize work manager", nfe);
		}

		// establish communication with the home process
		this.wep = new WorkerEndPoint(this,logger);
		StreamReader sr  = new StreamReader(new InputStreamReader(System.in),logger,wep) {
			@Override
			public void run() {
				try {
					super.run();
				} finally {
					// home died (probably). stop
					logger.info("Lost connection with home process. Going down now.");
					stop();
				}
			}
		};
		Thread t = new Thread(sr, this.workerName+"/in_reader");
		t.setDaemon(true);
		t.start();
		try {
			try {
				synchronized (this) {
					if (!started && !stopping) {
						this.loadDependencies();

						// get the Worker Runnable... if any.
						IResourceHandle rh = IComponentsLookup.INSTANCE.lookup(this.workerComponent, IResourceHandle.class);
						Runnable r = rh.as(Runnable.class);
						if (r!=null) {
							// make the worker soul depending on "main"
							this.handle().addDependency(rh);
							logger.fine("Calling worker component main method: "+this.workerName);
							r.run();
						}

						// commit any pending work
						WorkUnit.closeCurrent();
						started = true;
						logger.info("Completed worker process initialization");

						// TODO: add timeout condition for detached nodes
						this.wait();
						logger.info("Leaving Worker's Soul now");
					} else
						throw new IllegalStateException("Worker Process has been started or invalidated before!");
				}
			} finally {
				// close wep (so that pending messages will be flushed)
				wep.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Worker terminating with exception",e);
		}
	}

	/**
	 * Load worker up state. This is hard coded. This has to happen relatively early to
	 * make sure repositories are up
	 */
	private void loadWorkerUp() {
		loadDependencies(STATE_WORKER_UP);
	}

	/**
	 * Load all declared target states. We actually accept any dependency component - not just system
	 * states.
	 */
	private void loadDependencies() {
		String s = 	this.workerDesc().getProperties().getProperty(IWorkerProcess.STATES);
		if (s!=null) {
			StringTokenizer tk = new StringTokenizer(s,",");
			String[] ss = new String[tk.countTokens()];
			for (int i=0; tk.hasMoreElements(); i++) {
				ss[i]=tk.nextToken().trim();
			}
			loadDependencies(ss);
		}
	}

	/*
	 * Load a specified set of dependencies
	 */
	private void loadDependencies(String ... dependencies) {
		for (String s : dependencies) {
			IResourceHandle rh = IComponentsLookup.INSTANCE.lookup(s, IResourceHandle.class);
			try {
				IDependencyComponent dc = rh.as(IDependencyComponent.class);
				if (dc==null) {
					throw new IllegalStateException("Dependency component " + s +" not found or not supported");
				}
				dc.prepare();
				this.deps.add(rh);
			} catch (Throwable e) {
				if (e instanceof VirtualMachineError) {
					throw (VirtualMachineError) e;
				}
				if (!Foundation.isDevelopmentMode()) {
					throw new RuntimeException("Failed to satisfy worker target dependency \""+s+"\"",e);
				}
				logger.log(Level.SEVERE,"Failed to satisfy worker target dependency \""+s+"\"",e);
			}
		}
	}

	/**
	 * Handling of messages sent from home. This is about
	 * <ul>
	 * <li>termination,</li>
	 * <li>pinging</li>
	 * <li>handling invaliations</li>
	 * <li>Detaching, Attaching</li>
	 * <li>attaining target states</lI>
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	public Map<String,Serializable> processMessage(Map<String, Serializable> args) throws IOException {
		String group = (String) args.get(COMMAND_GROUP);
		String command = (String) args.get(COMMAND);
		Map<String, Serializable> result = null;
		synchronized (this) {
			if (stopping) {
				logger.finer("Worker received message while stopping. Message ignored");
				if (result==null)
					result = new HashMap<String, Serializable>();
				return result;
			}
		}
		HomeHandle.instance().getWorkerLease().increaseLease();
		try {
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("Received command " + command+ (group==null? "":" of group "+group));
			}
			if (group==null) {
				if (COMMAND_TERM.equals(command)) {
					// termination!
					stop();
					logger.info("Received TERM command");
				} else if (COMMAND_PING.equals(command)) {
					result = new HashMap<String, Serializable>();
					// say we are fine
					result.put("oki","doki");
				} else if (COMMAND_DETACH.equals(command)) {
					detach();
				} else if (COMMAND_GETLEASE.equals(command)) {
					result = new HashMap<String, Serializable>();
					result.put("lease", Integer.valueOf(this.lease));
				} else if (COMMAND_INVALIDATE.equals(command)) {
					Collection<String> s = (Collection<String>) args.get(INVALIDATIONSET);
					logger.fine("Checking " + s.size()
							+ " invalidation candidate resources");
					int i=IResourceManager.INSTANCE.invalidate(s);
					logger.info("Invalidated " + i + " resources");
				} else if (COMMAND_ATTAINOP.equals(command)) {
					this.loadWorkerUp();
					this.loadDependencies();
				} else {
					logger.warning("Unrecognized command: "+command);
				}
			} else {
				result = MessageHandlerResource.forwardMessageToHandler(logger,args);
			}
			if (result==null)
				result = new HashMap<String, Serializable>();
		} catch (Exception e) {
			throw new MessageException("Exception when trying to handle message on worker",e);
		} finally {
			HomeHandle.instance().getWorkerLease().decreaseLease();
		}
		return result;
	}

	private String getPid() {
		if (this.pid==null) {
			RuntimeMXBean rmx = ManagementFactory.getRuntimeMXBean();
			String name = rmx.getName();
			// the general scheme is <pid>@...
			int p = name.indexOf('@');
			if (p>0) {
				this.pid = name.substring(0,p);
			}
		}
		return pid;
	}
	
	private final static Logger logger = Logger.getLogger(WorkerSoul.class.getName());

}
