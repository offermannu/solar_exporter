/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.worker;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.zfabrik.workers.home.IHomeLayout;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * The Home Handle provides ways of interaction with the currently running home layout (see {@link IHomeLayout})
 * from a worker process perspective.
 * <p>
 * This interface allows to send messages to other worker processes, trigger a synchronization and more.
 *
 */
public abstract class HomeHandle {
	
	protected static HomeHandle instance;

	protected static synchronized void setInstance(HomeHandle h) {
		if (instance!=null) throw new IllegalStateException("Home Handle cannot be set twice!");
		instance = h;
	}
	
	
	public static synchronized HomeHandle instance() {
		return instance;
	}
	
	/**
	 * send a message. See also {@link IWorkerProcess}
	 * @param args
	 * @param timeout
	 * @return
	 * @throws IOException
	 */
	public abstract Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException;
	
	public abstract void triggerSynchronization(int scope) throws IOException;
	public abstract void triggerVerification() throws IOException;
	public abstract List<String> getLastSynchronizationLog() throws IOException;
	
	/**
	 * post resource invalidations by fully qualified resource names. These may be processed asynchronously. If local invalidation
	 * is mandatory, local invalidations should be forced additionally {@see IResourceManager#invalidate(Set)}
	 * @param invs
	 */
	public abstract void broadcastInvalidations(Collection<String> invs, int scope);
	
	/**
	 * The worker lease is a way to control a worker's life time beyond a detach situation.
	 * This can be used to implement session based worker life time extension for example.
	 */
	public abstract IWorkerLease getWorkerLease();
}