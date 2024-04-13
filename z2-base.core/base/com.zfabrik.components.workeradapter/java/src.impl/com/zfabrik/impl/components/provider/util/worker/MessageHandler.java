/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.provider.util.worker;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.util.sync.ISynchronizer;
import com.zfabrik.workers.IMessageHandler;

/**
 * Synchronization logic for worker processes as far as using the default component
 * repository implementation.
 * The home process broadcasts invalidation and unavailable
 * messages to this end point so that we can lock the component
 * repository and invalidate affected resources.
 * After a repository sync, the component repository can be made available
 * again.
 * This code runs on the worker process
 *
 * @author Henning Blohm
 *
 */
public class MessageHandler implements IMessageHandler {
	public final static String COMMAND_COMPLETE = "complete";
	public final static String GROUP = "com.zfabrik.fscr";


	public  Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		String command = (String) args.get(IMessageHandler.COMMAND);
		if (COMMAND_COMPLETE.equals(command)) {
			// copy offline mode setting
			String offline = (String) args.get(Foundation.OFFLINE);
			if (Boolean.parseBoolean(offline)) {
				logger.info("Worker in offline mode");
			}
			System.setProperty(Foundation.OFFLINE, offline);
			logger.fine("Received complete() for AbstractComponentRepository repositories");
			ISynchronizer s = IComponentsLookup.INSTANCE.lookup("com.zfabrik.boot.repos/synchronizer",ISynchronizer.class);
			if (s!=null) {
				s.complete(null);
			} else {
				logger.log(Level.WARNING,"Synchronizer for AbstractComponentRepository implementations not found");
			}
		}
		return null;
	}

	private final static Logger logger = Logger.getLogger(MessageHandler.class.getName());
}
