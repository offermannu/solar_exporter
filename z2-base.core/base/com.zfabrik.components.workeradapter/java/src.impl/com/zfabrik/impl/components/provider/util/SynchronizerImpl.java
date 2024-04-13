/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.provider.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.zfabrik.impl.components.provider.util.worker.MessageHandler;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.util.sync.ISynchronization;
import com.zfabrik.util.sync.ISynchronizer;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;

/**
 * This runs on home and complements the basic synchronization for worker processes.
 *
 * The basic worker synchronizer handles invalidations and target state attaining. This synchronizer
 * does repository specific stuff in between by sending an early complete to the workers so they drop
 * their in-memory repository cache and more.
 *
 */

public class SynchronizerImpl implements ISynchronizer {

	public void complete(ISynchronization sync) {
		logger.fine("Sending complete() to worker processes");
		Map<String, Serializable> args = new HashMap<String, Serializable>();
		args.put(IMessageHandler.COMMAND, MessageHandler.COMMAND_COMPLETE);
		args.put(IMessageHandler.COMMAND_GROUP, MessageHandler.GROUP);
		// pass on offline mode that can be changed on the fly.
		args.put(Foundation.OFFLINE, Boolean.toString(Foundation.isOfflineMode()));

		try {
			IHomeLayout h = IHomeLayout.get();
			if (h!=null) {
				h.broadcastMessage(args, -1);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Error during sending of complete() to worker processes",e);
		}
	}

	public void preInvalidation(ISynchronization sync) {
	}

	private final static Logger logger = Logger.getLogger(SynchronizerImpl.class.getName());
}
