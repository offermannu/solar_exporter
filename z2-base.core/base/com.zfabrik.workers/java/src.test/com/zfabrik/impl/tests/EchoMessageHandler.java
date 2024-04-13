/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.tests;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.zfabrik.workers.IMessageHandler;

public class EchoMessageHandler implements IMessageHandler {

	@Override
	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		Map<String,Serializable> m = new HashMap<String, Serializable>();
		m.put("out",args.get("in"));
		return m;
	}

}
