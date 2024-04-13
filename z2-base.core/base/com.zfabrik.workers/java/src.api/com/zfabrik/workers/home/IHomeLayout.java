/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.home;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.workers.worker.HomeHandle;

/**
 * The home layout manages all running worker processes if any. Worker processes can be started
 * by simply _preparing_ (see {@link IDependencyComponent}) a worker process component. 
 * <p>
 * This interface allows to send messages to worker processes from the home process. It is
 * hence mostly used internally and should not be used be applications.
 * <p>
 * See {@link HomeHandle} for a worker process perspective onto the running
 * home layout.
 */
public interface IHomeLayout {
	/**
	 * broadcast scope: local home process
	 */
	int SCOPE_HOME = 1;
	/**
	 * broadcast scope: all worker processes in home
	 */
	int SCOPE_WORKERS = 2;
	/**
	 * broadcast scope: exclude the sending worker process
	 */
	int SCOPE_OTHERS  = 4;

	/**
	 * broadcast scope: All of the home layout
	 */
	public final static int SCOPE_HOME_LAYOUT = SCOPE_HOME | SCOPE_WORKERS;

	/**
	 * Retrieve the home layout instance
	 */
	static IHomeLayout get() {
		IHomeLayout hl = IComponentsLookup.INSTANCE.lookup("com.zfabrik.workers/homeLayout", IHomeLayout.class);
		if (hl==null) {
			throw new IllegalStateException("Home layout not found");
		}
		return hl;
	}

	/**
	 * broadcast a message to all worker processes of a home. Timeouts will result in exceptions 
	 * @param args
	 * @param timeout timeout per worker or -1 for default
	 * @return
	 * @throws IOException
	 */
	void broadcastMessage(Map<String, Serializable> args, long timeout) throws IOException;

	/**
	 * broadcast a message to all worker processes of a home 
	 * @param args
	 * @param timeout timeout per worker or -1 for default
	 * @param killOnTimeOut if <code>true</code> a time out will lead to a worker process termination.
	 * @return
	 * @throws IOException
	 */
	void broadcastMessage(Map<String, Serializable> args, long timeout,boolean killOnTimeOut) throws IOException;

	/**
	 * broadcast resource invalidations to all worker processes of a home and, if specified, to all 
	 * homes and workers in a cluster. Invalidations will be processed on the home process as well and before
	 * distribution to worker processes.
	 * 
	 * @param args
	 * @param timeout timeout per worker or -1 for default
	 * @return
	 * @throws IOException
	 */
	void broadcastInvalidations(Collection<String> invs, long timeout, int scope, String senderWorker) throws IOException;
}
