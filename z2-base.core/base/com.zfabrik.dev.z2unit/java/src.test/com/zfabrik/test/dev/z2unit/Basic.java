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
import org.junit.runner.notification.Failure;

import com.zfabrik.test.dev.z2unit.test.TestClass;
import com.zfabrik.test.dev.z2unit.test.TestClassWithFail;
import com.zfabrik.test.dev.z2unit.test.TestClassWithParameters;
import com.zfabrik.test.dev.z2unit.util.MethodNameFilter;
import com.zfabrik.test.dev.z2unit.util.TrackingRunNotifier;
import com.zfabrik.test.dev.z2unit.util.TrackingRunNotifier.Event;
import com.zfabrik.test.dev.z2unit.util.Z2UnitTestTest;
import com.zfabrik.z2unit.Z2UnitTestRunner;

/**
 * Basic z2unit tests. We call a designated test class and check the output
 */
public class Basic extends Z2UnitTestTest {
	
	
	@Test
	public void testClass() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClass.class);
		r.run(trackingRunNotifier);
		// ok... we should have
		verifySequence(0,true,
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
	}

	@Test
	public void testMethod() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClass.class);
		// filter for method
		r.filter(new MethodNameFilter("test1"));
		r.run(trackingRunNotifier);
		// ok... we should have
		verifySequence(0,true,
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
	}

	@Test
	public void testClassWithFailedMethod() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClassWithFail.class);
		r.run(trackingRunNotifier);
		// ok... we should have
		verifySequence(0,true,
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFailure,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
		// check the failure
		Failure f = trackingRunNotifier.getEvents().get(4).getFailure();
		Assert.assertEquals(AssertionError.class, f.getException().getClass());
	}
	
	
	@Test
	public void testClassWithParameterized() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClassWithParameters.class);
		r.run(trackingRunNotifier);
		// ok... we should have
		verifySequence(0,true,
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
		// check the descriptions
		Event e = trackingRunNotifier.getEvents().get(1);
		Assert.assertEquals("test1[0]",e.getDescription().getMethodName());
		e = trackingRunNotifier.getEvents().get(3);
		Assert.assertEquals("test2[0]",e.getDescription().getMethodName());
		e = trackingRunNotifier.getEvents().get(5);
		Assert.assertEquals("test1[1]",e.getDescription().getMethodName());
		e = trackingRunNotifier.getEvents().get(7);
		Assert.assertEquals("test2[1]",e.getDescription().getMethodName());
		
	}	
	
	// -----------------
	
}
