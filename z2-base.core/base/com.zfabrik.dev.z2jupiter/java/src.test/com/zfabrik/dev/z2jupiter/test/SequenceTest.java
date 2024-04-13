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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;
import com.zfabrik.resources.IResourceManager;

/**
 * A simple test on server side test sequence (which should really
 * anyway be unaltered by Z2 Jupiter)
 */
@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
public class SequenceTest {
	private final static Logger LOG = Logger.getLogger(SequenceTest.class.getName());
	private static boolean beforeAll, afterAll;
	private boolean before, first, second, after;

	@BeforeAll
	static void beforeAll() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("beforeAll");
		Assertions.assertFalse(beforeAll);
		beforeAll=true;
		afterAll=false;
	}
	
	@BeforeEach
	public void before() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("before");
		Assertions.assertTrue(beforeAll);
		Assertions.assertFalse(before);
		before=true;
	}

	@Test
	@DisplayName("First Test")
	void oneTest() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("oneTest");
		Assertions.assertTrue(before);
		Assertions.assertFalse(first);
		first = true;
		assertEquals(30, 5* 6, "6 * 5 will be 30");
	}

	@Test
	@DisplayName("Second Test")
	void twoTest() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("twoTest");
		Assertions.assertTrue(before);
		Assertions.assertFalse(second);
		second = true;
		assertEquals(30, 5* 6, "6 * 5 will be 30");
	}

	@AfterEach
	public void after() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("after");
		Assertions.assertTrue(first || second);
		Assertions.assertFalse(after);
		Assertions.assertFalse(afterAll);
		after = true;
	}
	
	@AfterAll
	static void afterAll() {
		Assertions.assertNotNull(IResourceManager.INSTANCE);
		LOG.info("afterAll");
		Assertions.assertTrue(beforeAll);
		Assertions.assertFalse(afterAll);
		afterAll=true;
		beforeAll=false;
	}
}
