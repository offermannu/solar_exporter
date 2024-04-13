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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

/**
 * Run with nested parameterized
 */
@RunWith(Z2UnitTestRunner.class)
@Z2UnitTest(
	componentName="com.zfabrik.dev.z2unit",
	runWith=Parameterized.class
)
//@Ignore
public class TestClassWithParameters {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { {"1",1}, {"2",2}});
    }

	private String param1;
	private int param2;
	
	public TestClassWithParameters(String param1, int param2) {
		super();
		this.param1 = param1;
		this.param2 = param2;
	}

	@Test
	public void test1() {
		Assert.assertEquals(Integer.parseInt(param1),param2);
	}
	
	@Test
	public void test2() {
		Assert.assertEquals(Integer.parseInt(param1),param2);
	}
	
}
