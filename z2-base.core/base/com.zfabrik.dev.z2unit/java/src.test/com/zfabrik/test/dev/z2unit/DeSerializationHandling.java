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

import com.zfabrik.test.dev.z2unit.test.TestClassWithNonSerializableException;
import com.zfabrik.test.dev.z2unit.util.MethodNameFilter;
import com.zfabrik.test.dev.z2unit.util.TrackingRunNotifier;
import com.zfabrik.test.dev.z2unit.util.Z2UnitTestTest;
import com.zfabrik.z2unit.RemoteErrorException;
import com.zfabrik.z2unit.Z2UnitTestRunner;

/**
 * As a principle, Junit wants to have throwables as causes for
 * failures in tests. We do a best effort to serialize and de-serialize from remote.
 * However, sometimes this does not work, in which case we fall back to showing text stacktraces 
 * wrapped in a local RuntimeException.
 */
public class DeSerializationHandling extends Z2UnitTestTest {

	@Test
	public void nonSerializableHandled() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClassWithNonSerializableException.class);
		r.filter(new MethodNameFilter("nonSerializable"));
		r.run(trackingRunNotifier);
		verifySequence(0, true, 
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFailure,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
		Failure f = trackingRunNotifier.getEvents().get(2).getFailure();
		Assert.assertNotNull(f);
		// we failed to serialize. The throwable contained should
		// be a local wrapper exception
		Throwable t = f.getException();
		Assert.assertNotNull(t);
		Assert.assertEquals(RemoteErrorException.class,t.getClass());	
		Assert.assertTrue(((RemoteErrorException)t).getRemoteStackTrace().startsWith("com.zfabrik.test.dev.z2unit.test.TestClassWithNonSerializableException$NonSerializableException"));
	}
	
	@Test
	public void nonDeSerializableHandled() throws Exception {
		Z2UnitTestRunner r = new Z2UnitTestRunner(TestClassWithNonSerializableException.class);
		r.filter(new MethodNameFilter("nonDeSerializable"));
		r.run(trackingRunNotifier);
		verifySequence(0, true, 
			TrackingRunNotifier.EventType.fireTestRunStarted,
			TrackingRunNotifier.EventType.fireTestStarted,
			TrackingRunNotifier.EventType.fireTestFailure,
			TrackingRunNotifier.EventType.fireTestFinished,
			TrackingRunNotifier.EventType.fireTestRunFinished
		);
		Failure f = trackingRunNotifier.getEvents().get(2).getFailure();
		Assert.assertNotNull(f);
		// we failed to serialize. The throwable contained should
		// be a local wrapper exception
		Throwable t = f.getException();
		Assert.assertNotNull(t);
		Assert.assertEquals(RemoteErrorException.class,t.getClass());	
		Assert.assertTrue(((RemoteErrorException)t).getRemoteStackTrace().startsWith("com.zfabrik.test.dev.z2unit.test.TestClassWithNonSerializableException$NonDeSerializableException"));
	}

}
