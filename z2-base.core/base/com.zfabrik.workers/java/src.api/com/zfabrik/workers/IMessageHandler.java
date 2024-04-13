/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers;

import java.io.Serializable;
import java.util.Map;

public interface IMessageHandler {
	final static String TYPE = "com.zfabrik.worker.MessageHandler";
	
	/**
	 * constant used to identify a message handler by command group
	 */
	final static String COMMAND_GROUP = "com.zfabrik.worker.commandGroup";
	/**
	 * proposal constant to transport a command identifier if used for RPC
	 */
	final static String COMMAND = "com.zfabrik.worker.command";

	/**
	 * proposal constant to be used to transport message return values
	 */
	final static String RETURN = "com.zfabrik.worker.returnValue";

	/**
	 * 
	 * @param args
	 * @return
	 */
	Map<String,Serializable> processMessage(Map<String,Serializable> args) throws Exception; 

}
