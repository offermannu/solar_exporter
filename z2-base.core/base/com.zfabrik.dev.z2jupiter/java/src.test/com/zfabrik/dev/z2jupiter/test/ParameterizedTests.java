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

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;

@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
public class ParameterizedTests {
	
    @ParameterizedTest
    @ValueSource(ints = {8,4,2,6,10})
    void test_int_arrays(int arg) {
      System.err.println("arg => "+arg);
      assertTrue(arg % 2 == 0);
    }   
}
