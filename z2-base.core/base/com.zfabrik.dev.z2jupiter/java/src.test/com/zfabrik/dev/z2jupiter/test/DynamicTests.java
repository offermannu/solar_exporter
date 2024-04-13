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

import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;

@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
public class DynamicTests {
	
	@TestFactory
	Collection<DynamicTest> dynamicTests() {
	    return Arrays.asList(
	      DynamicTest.dynamicTest("Add test", () -> assertEquals(2, Math.addExact(1, 1))),
	      DynamicTest.dynamicTest("Multiply Test", () -> assertEquals(4, Math.multiplyExact(2, 2)))
        );
	}

}
