/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.components.query;

import com.zfabrik.resources.IResourceLookup;
import com.zfabrik.resources.IResourceManager;


/**
 * utility abstraction for lookup of package resources
 *
 *
 */
public interface IComponentQueriesLookup  {

	String NAMESPACE = "com.zfabrik.components.query";
	static IResourceLookup INSTANCE = initializer.get();
	
	// initialization trick
	public static class initializer {
		private static IResourceLookup get() {
			try {
				return IResourceManager.INSTANCE.lookup(NAMESPACE, IResourceLookup.class);
			} catch (Exception e) {
				RuntimeException t = new IllegalStateException("failed to load package lookup!",e);
				t.printStackTrace();
				throw t;
			}
		}
	}
	
	
}
