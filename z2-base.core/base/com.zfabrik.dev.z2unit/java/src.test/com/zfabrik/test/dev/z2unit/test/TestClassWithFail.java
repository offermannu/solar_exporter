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

import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

@RunWith(Z2UnitTestRunner.class)
@Z2UnitTest(componentName="com.zfabrik.dev.z2unit")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// this ignore is only client-effective. It will be ignored on the server side (via the runner)
@Ignore
public class TestClassWithFail {

	@Test
	public void test1() {
		
	}
	
	@Test(expected=RuntimeException.class)
	public void test2() {
		// this fails
	}
}
