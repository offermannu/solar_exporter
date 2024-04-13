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

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * Filter by method name
 */
public class MethodNameFilter extends Filter {
	private String methodName;

	public MethodNameFilter(String methodName) {
		super();
		this.methodName = methodName;
	}

	@Override
	public boolean shouldRun(Description description) {
		return description.isTest() && this.methodName.equals(description.getMethodName());
	}

	@Override
	public String describe() {
		return this.methodName;
	}

}
