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

import java.io.Serializable;
import java.util.Map;

import com.zfabrik.workers.IMessageHandler;

public class ComponentsMessageHandler implements IMessageHandler {
	
	public static final String GROUP = "com.zfabrik.components.query";
	public static final String VERIF_QUERIES = "verify_queries";

	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		String cmd = (String) args.get(IMessageHandler.COMMAND);
		if (VERIF_QUERIES.equals(cmd)) {
			QueryVerification.execute();
		}
		return null;
	}

}
