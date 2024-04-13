/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.engine;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient;
import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClientTestDescriptor;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestPlanDto;

/**
 * Client side representation of a test class based descriptor of a test class that was
 * annotated using {@link Z2JupiterTestable}.
 */
public class Z2JupiterRemoteTestDescriptor extends AbstractTestDescriptor {
	private Type type;
	private Z2JupiterTestPlanDto testPlan;
	private Z2JupiterClient client;
	
	public Z2JupiterRemoteTestDescriptor(UniqueId prefix, String displayName, TestSource source, Type type, Z2JupiterClient client, Z2JupiterTestPlanDto testPlan) {
		super(
			prefix,
			displayName,
			source
		);
		this.type = type;
		this.client = client;
		this.testPlan = testPlan;
		testPlan.getRoots().stream()
		// add our uniqueid as prefix
		.map(i->new Z2JupiterClientTestDescriptor(i,prefix)).forEach(this::addChild);
	}    
	
	@Override
	public void addChild(TestDescriptor child) {
		super.addChild(child);
	}
	
	@Override
	public Type getType() {
		return this.type;
	}
	
	public Z2JupiterTestPlanDto getTestPlan() {
		return testPlan;
	}
	
	public Z2JupiterClient getClient() {
		return client;
	}
	
	@Override
	public String toString() {
		return super.toString()+"/"+this.getType();
	}
}
