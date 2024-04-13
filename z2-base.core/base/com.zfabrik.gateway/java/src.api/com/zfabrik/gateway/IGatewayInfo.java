/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.gateway;

/**
 * Internal information access for gateway components.
 */
public interface IGatewayInfo {

	/**
	 * Get the name of the current target worker process. 
	 */
	String  getTargetWorkerName();
	
	/**
	 * Get the port used for internal dispatch
	 */
	int getGatewayPort(String workerProcessName);

}
