/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.dev.z2unit.util;

import org.junit.Assert;

import com.zfabrik.test.dev.z2unit.util.TrackingRunNotifier.EventType;

/**
 * super class simplifying z2unit test tests
 */
public class Z2UnitTestTest {
	protected TrackingRunNotifier trackingRunNotifier = new TrackingRunNotifier();

	/**
	 * Verify a sequence of events.
	 */
	protected  void verifySequence(int offset,boolean conclusive, EventType ... events) {
		int l = trackingRunNotifier.getEvents().size();
		for (int j=0;j<events.length;j++) {
			if (l<=j+offset) {
				Assert.fail("Expected event at index "+(j+offset)+ " but found no more");
			}
			EventType expected=events[j], actual=trackingRunNotifier.getEvents().get(j+offset).getType();
			
			Assert.assertEquals(
				"Expected event of type "+expected+" at position "+(offset+j)+" but found "+actual,
				expected,
				actual				
			);
		}
		int t = offset+events.length;
		if (conclusive) {
			Assert.assertTrue("Expected no more events after position "+(t-1)+" but found "+(t-l)+" more",t==l);
		}
	}
	

}
