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

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import com.zfabrik.workers.IMessageHandler;

/**
 * This is the last mile of the HubCR scanOnSync flow. As the synchronizer runs on
 * home, we send a message to offload the work to a worker
 * 
 * @author hb
 *
 */
public class MessageHandlerImpl implements IMessageHandler {
	@Override
	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		String cmd = (String) args.get(COMMAND);
		if (SynchronizerImpl.SCAN.equals(cmd)) {
			HubCRResource.getResource().scanOnSync();
		}
		return Collections.emptyMap();
	}

}
