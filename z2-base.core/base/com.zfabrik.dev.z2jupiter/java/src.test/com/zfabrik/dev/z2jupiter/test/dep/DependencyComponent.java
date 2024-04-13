/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.test.dep;

import com.zfabrik.components.IDependencyComponent;

public class DependencyComponent implements IDependencyComponent {
	public static long prepTime = 0;
	
	@Override
	public synchronized void prepare() {
		prepTime=System.currentTimeMillis();
	}

	public synchronized static long getPrepTime() {
		return prepTime;
	}
}
