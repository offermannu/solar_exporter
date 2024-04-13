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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.workers.home.IWorkerHorde;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * A worker process resource manages zero or more running worker 
 * instances. The normal case is zero or one. But when
 * using the Gateway feature, there may be previously started still active 
 * worker processes.
 */
public class WorkerProcessResource extends Resource implements IWorkerHorde, IDependencyComponent {
	/**
	 * This number gets incremented with every new worker process instance. It is used to 
	 * compute port numbers for JMX, debug, etc. Also it is part of the 
	 * qualified worker process name as exposed in JMX.  See also {@link IWorkerHorde}.
	 */
	private int topVariantNum=0;
	
	private synchronized int getFreeVariant() {
		return topVariantNum++;
	}
	
	private WorkerProcess active;
	private String name;
	private Map<String, IWorkerProcess> processes = new HashMap<String, IWorkerProcess>();
	
	public WorkerProcessResource(String name) {
		super(); 
		this.name = name; 
	}

	public synchronized <T> T as(Class<T> clz) {
		_cleanup();
		if (IWorkerProcess.class.equals(clz)) {
			_load();
			return clz.cast(this.active); 
		}
		if (IWorkerHorde.class.equals(clz)) {
			return clz.cast(this);
		}
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}

	private void _homeOnly() {
		if (Foundation.isWorker()) {
			throw new IllegalStateException("Cannot provide worker process interface on worker node");
		}
	}

	private void _load() {
		_homeOnly();
		if (this.active==null || this.active.isDetached()) {
			logger.fine("Creating new (active) worker process instance: "+this.name);
			// create a new "latest" one
			this.active = new WorkerProcess(name,IComponentsManager.INSTANCE.getComponent(name),getFreeVariant());
			this.processes.put(this.active.getName(),this.active);
			handle().adjust(0, Long.MAX_VALUE, IResourceHandle.HARD);
		}
	}

	private void _cleanup() {
		// remove process instances that are not running anymore
		Iterator<Map.Entry<String, IWorkerProcess>> i = this.processes.entrySet().iterator();
		while (i.hasNext()) {
			IWorkerProcess p = i.next().getValue();
			if (!p.equals(this.active) && !p.isRunning()) {
				// it's not the current active, nor is it running. Remove it from management.
				i.remove();
			}
		}
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		try {
			// stop all!
			Iterator<Map.Entry<String, IWorkerProcess>> i = this.processes.entrySet().iterator();
			while (i.hasNext()) {
				IWorkerProcess p = null;
				try {
					p = i.next().getValue();
					if (p!=null) {
						p.stop();
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error stopping worker process "+p.getName());
				} finally {
					i.remove();
				}
			}
		} finally {
			// reset
			this.active = null;
			this.topVariantNum=0;
		}
	}
	
	@Override
	public void prepare() {
		IWorkerProcess wp = null;
		synchronized (this) {
			this._load();
			if (this.active.getState()==IWorkerProcess.NOT_STARTED) {
				// start it now
				wp = this.active;
			}
		}
		if (wp!=null) {
			wp.start();
		}
	}
	
	@Override
	public synchronized IWorkerProcess getActiveWorkerProcess() {
		_load();
		return this.active;
	}
	@Override
	public String getComponentName() {
		return this.name;
	}
	@Override
	public Map<String, IWorkerProcess> getWorkerProcesses() {
		return Collections.unmodifiableMap(new HashMap<String, IWorkerProcess>(this.processes));
	}
	
	private final static Logger logger = Logger.getLogger(WorkerProcessResource.class.getName());
}
