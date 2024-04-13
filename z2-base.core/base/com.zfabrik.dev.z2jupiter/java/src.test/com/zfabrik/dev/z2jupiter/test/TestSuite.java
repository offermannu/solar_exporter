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
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.RunWith;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;
import com.zfabrik.dev.z2jupiter.test.suite.Suite1Test;
import com.zfabrik.dev.z2jupiter.test.suite.Suite2Test;

@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
@RunWith(JUnitPlatform.class)
@SelectClasses({Suite1Test.class,Suite2Test.class})
public class TestSuite {
	@Test
	public void doesNothing() {
	}

}
