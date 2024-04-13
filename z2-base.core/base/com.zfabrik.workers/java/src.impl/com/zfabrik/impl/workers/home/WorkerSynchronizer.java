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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.zfabrik.impl.workers.WorkerConstants;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.util.sync.ISynchronization;
import com.zfabrik.util.sync.ISynchronizer;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;

/**
 * This synchronizer takes care that worker processes receive invalidations and attain their target states again.
 */
public class WorkerSynchronizer implements ISynchronizer {

	// if its not back up after 10 min. we will kill it!
	private final static String TO_ATTAINOP = "com.zfabrik.home.attainOperationTimeOut";
	private final static String TO_ATTAINOP_DEF = "600000";

	public void complete(ISynchronization sync) {
		// forward invalidation set to worker
		IHomeLayout h = IHomeLayout.get();
		if (h!=null) {
			if (sync.getInvalidationSet().size()>0) {
				logger.fine("Sync: Broadcasting invalidation set to worker processes");
				try {
					h.broadcastInvalidations(sync.getInvalidationSet(), -1, IHomeLayout.SCOPE_WORKERS,null);
				} catch (IOException e) {
					throw new IllegalStateException("Error during send of invalidation message to worker processes",e);
				}
			}
			// tell workers to attain operational states again
			long to = Long.parseLong(Foundation.getProperties().getProperty(TO_ATTAINOP,TO_ATTAINOP_DEF).trim());
			logger.fine("Sync: Re-establishing worker target states");
			Map<String,Serializable> args = new HashMap<String, Serializable>();
			args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_ATTAINOP);
			try {
				h.broadcastMessage(args, to ,true);
			} catch (IOException e) {
				throw new IllegalStateException("Error during send of worker_up message to worker processes",e);
			}
		}
	}

	public void preInvalidation(ISynchronization sync) {}

	private final static Logger logger = Logger.getLogger(WorkerSynchronizer.class.getName());
}
