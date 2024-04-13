/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.actions;


public interface IActionProvider {

	// a map holding either another map or an IRequestHandler
	IAction getAction(String uri);
	
}
