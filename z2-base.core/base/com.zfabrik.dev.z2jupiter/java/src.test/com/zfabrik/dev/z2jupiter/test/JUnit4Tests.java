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


import org.junit.Test;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;

@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
public class JUnit4Tests {

	@Test
	public void testMe() {
		System.err.println("hello testable Junit 4");
	}

	@Test
	public void testYou() {
		System.err.println("hello testable Junit 4 again");
	}

}