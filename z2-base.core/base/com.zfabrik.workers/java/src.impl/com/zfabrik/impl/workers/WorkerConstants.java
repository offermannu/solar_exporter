/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers;

/**
 * Command line properties for worker processes
 * 
 * @author hb
 * 
 * 
 */
public interface WorkerConstants {
	final static String COMMAND_TERM = "terminate";
	final static String COMMAND_PING = "ping";
	final static String COMMAND_DETACH = "detach";
	final static String COMMAND_INVALIDATE = "invalidate";
	final static String COMMAND_ATTAINOP = "attainop";
	final static String INVALIDATIONSET = "is";
	final static String COMMAND_GETLEASE = "getLease";
}
