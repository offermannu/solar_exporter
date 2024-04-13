package com.zfabrik.dev.z2jupiter.test.suite;

import org.junit.jupiter.api.Test;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.impl.Z2JupiterImpl;

@Z2JupiterTestable(componentName = Z2JupiterImpl.MODULE_NAME)
public class Suite1Test {
	
	@Test
	public void suite1Test() {
		System.err.println("suite1Test");
	}
}
