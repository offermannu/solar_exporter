/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.management.home;

import com.zfabrik.components.IComponentsLookup;


/**
 * 
 * @author hb
 *
 */
public interface IHomeJMXClient {

	public final static IHomeJMXClient INSTANCE = Initializer.get();
	
	final static class Initializer {
		private static IHomeJMXClient get() {
			return IComponentsLookup.INSTANCE.lookup("com.zfabrik.management/homeJMXClient", IHomeJMXClient.class);
		}
	}
	
	/**
	 * provides a remote Mbean server access.
	 * 
	 * @param workerComponent name of the worker process component or <code>null</code> for the home process
	 * @return 
	 */
	IHomeMBeanServer getRemoteMBeanServer(String workerComponent);
	
}
