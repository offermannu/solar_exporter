/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.jupiter.engine.descriptor.TestTemplateTestDescriptor;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestTag;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.launcher.TestPlan;

import com.zfabrik.dev.z2jupiter.internal.engine.Z2JupiterRemoteTestDescriptor;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestIdentifierDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestSourceDto;
import com.zfabrik.dev.z2jupiter.internal.util.UniqueIds;

/**
 * Client side {@link TestDescriptor} of a server side test. The descriptor
 * does not hold its execution, it is merely descriptive to the client as part
 * of a {@link TestPlan}. Execution is implemented either via the custom  launcher
 * or when running into {@link Z2JupiterRemoteTestDescriptor}.
 */
public class Z2JupiterClientTestDescriptor extends AbstractTestDescriptor {
	private final static Logger LOG = Logger.getLogger(Z2JupiterClientTestDescriptor.class.getName());

	/*
	 * We use some heuristics what tests may register tests dynamically. JUnit removes everything from the
	 * test plan that holds no actual tests. However the test identifier exposed to the client does not 
	 * model dynamic tests accordingly by hiding the mayRegisterTest information. So we conclude for dynamic
	 * tests via a segment type in the unique name of the descriptor.
	 */
	private final static Set<String> DYNAMIC_SEGMENT_TYPES = new HashSet<>(Arrays.asList(
		TestFactoryTestDescriptor.DYNAMIC_CONTAINER_SEGMENT_TYPE,
		TestFactoryTestDescriptor.DYNAMIC_TEST_SEGMENT_TYPE,
		TestFactoryTestDescriptor.SEGMENT_TYPE,
		TestTemplateTestDescriptor.SEGMENT_TYPE
	));
	
	private Type type;
	private Set<TestTag> tags;  
	private boolean mayRegisterTests;
	
	public Z2JupiterClientTestDescriptor(Z2JupiterTestIdentifierDto tiDto) {
		this(tiDto,null);
	}
	
	public Z2JupiterClientTestDescriptor(Z2JupiterTestIdentifierDto tiDto, UniqueId prefix) {
		super(
			UniqueIds.concat(prefix,UniqueId.parse(tiDto.getUniqueId())),
			tiDto.getDisplayName(),
			Optional.ofNullable(tiDto.getSource()).map(Z2JupiterTestSourceDto::toTestSource).orElse(null)
		);
		this.type = tiDto.getType();
		tiDto.getChildren().stream().map(i->new Z2JupiterClientTestDescriptor(i,prefix)).forEach(this::addChild);
		this.tags = tiDto.getTags();
		
		this.mayRegisterTests = DYNAMIC_SEGMENT_TYPES.contains(getUniqueId().getLastSegment().getType());
	}    
	
	@Override
	public void addChild(TestDescriptor child) {
		LOG.fine("Adding "+child); 
		super.addChild(child);
	}

	@Override
	public boolean mayRegisterTests() {
		return this.mayRegisterTests;
	}

	@Override
	public Set<TestTag> getTags() {
		return this.tags;
	}


	@Override
	public Type getType() {
		return this.type;
	}
	
	@Override
	public String toString() {
		return super.toString()+"/"+this.getType();
	}

}
