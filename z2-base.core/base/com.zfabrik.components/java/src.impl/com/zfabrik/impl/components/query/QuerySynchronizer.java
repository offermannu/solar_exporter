/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.query;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.zfabrik.util.sync.ISynchronization;
import com.zfabrik.util.sync.ISynchronizer;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;


public class QuerySynchronizer implements ISynchronizer {

	public void complete(ISynchronization sync) {
		IHomeLayout h = IHomeLayout.get();
		if (h!=null) {
			// check also in home
			QueryVerification.execute();
			// send verify to all remaining worker processes
			try {
				Map<String,Serializable> args = new HashMap<String, Serializable>();
				args.put(IMessageHandler.COMMAND_GROUP, ComponentsMessageHandler.GROUP);
				args.put(IMessageHandler.COMMAND, ComponentsMessageHandler.VERIF_QUERIES);
				h.broadcastMessage(args, -1);
			} catch (IOException e) {
				throw new IllegalStateException("Error during send of invalidation message to worker processes",e);
			}
		}
	}

	public void preInvalidation(ISynchronization sync) {	}

	
}
