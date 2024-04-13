/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.dev.z2unit;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.model.InitializationError;

import com.zfabrik.test.dev.z2unit.test.TestClassWithInitializationError;
import com.zfabrik.test.dev.z2unit.util.Z2UnitTestTest;
import com.zfabrik.z2unit.Z2UnitTestRunner;

public class DoNotHideInitErrors extends Z2UnitTestTest {

	@Test
	public void initErrorNotWrapped() throws InitializationError {
		try {
			Z2UnitTestRunner r = new Z2UnitTestRunner(TestClassWithInitializationError.class);
			Assert.fail("Expected InitializationError");
		} catch (InitializationError ie) {
			Assert.assertTrue(ie.getCauses().size()==1);
			Throwable e = ie.getCauses().get(0);
			Assert.assertNotNull(e);
			Assert.assertNotNull(e.getMessage());
			Assert.assertTrue(e.getMessage().contains("Method notTestableAsItIsPrivate() should be public"));
		}
	}
	
}
