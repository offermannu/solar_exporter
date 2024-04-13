/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.hubcr.store;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.util.sync.ISynchronization;
import com.zfabrik.util.sync.ISynchronizer;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;

/**
 * A synchronizer that simply forwards to a shared
 * {@link HubCRResource} instance. If the HubCR is configured
 * to update at end of sync, this is the control flow handler for
 * that. If not configured, the synchronizer will still call but
 * no changes will be applied.
 * <p>
 * If you intent to not have hub cr be involved at any time,
 * uncomment the component type in the synchronizer definition.
 * <p>
 * Also make sure to not have the web front end started.
 *   
 * @author hb
 *
 */
public class SynchronizerImpl implements ISynchronizer {

	// 60 min. to do a scan 
	private static final int SCAN_TIMEOUT = 60*60*1000;
	public static final String SCAN = "scan";

	@Override
	public void preInvalidation(ISynchronization sync) {}

	@Override
	public void complete(ISynchronization sync) {
		if (HubCRResource.getResource().isScanOnSync()) {
			Map<String,Serializable> args = new HashMap<String, Serializable>();
			args.put(IMessageHandler.COMMAND, SCAN);
			args.put(IMessageHandler.COMMAND_GROUP, "com.zfabrik.hubcr");
			
			try {
				IHomeLayout.get().broadcastMessage(args,SCAN_TIMEOUT,true);
			} catch (IOException e) {
				logger.log(Level.SEVERE,"Problem completing HubCR scan on sync",e);
			}	
		}
	}
	
	private final static Logger logger = Logger.getLogger(SynchronizerImpl.class.getName());
}
