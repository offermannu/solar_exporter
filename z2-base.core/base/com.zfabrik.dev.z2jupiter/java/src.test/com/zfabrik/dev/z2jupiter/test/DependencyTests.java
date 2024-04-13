/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;
import com.zfabrik.dev.z2jupiter.test.dep.DependencyComponent;

@Z2JupiterTestable(
	componentName = Z2JupiterImpl.MODULE_NAME, 
	dependencies = { Z2JupiterImpl.MODULE_NAME + "/testDependency" }
)
public class DependencyTests {

	/**
	 * Check that a dependency has been prepared before the test
	 */
	@Test
	public void dependencyHasBeenInvoked() {
		Assertions.assertTrue(DependencyComponent.getPrepTime()>System.currentTimeMillis()-5000);
	}
}
