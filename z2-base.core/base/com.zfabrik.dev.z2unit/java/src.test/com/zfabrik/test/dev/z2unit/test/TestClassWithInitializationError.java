/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.dev.z2unit.test;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

/**
 * Test class for #1964 leading to an initialization error that shouldn't be 
 * hidden
 */
@RunWith(Z2UnitTestRunner.class)
@Z2UnitTest(componentName="com.zfabrik.dev.z2unit")
@Ignore
public class TestClassWithInitializationError {

	@Test
	private void notTestableAsItIsPrivate() {
		
	}
	
}
