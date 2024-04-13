/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.transfer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * DTO for test plan transport back from back end.
 * This does not really transport the test plan as that holds
 * implementation types from engines on the back end,
 * instead it provides a mimicked test plan that has the same identifiers
 * and can be served to the client for introspection.
 */
public class Z2JupiterTestPlanDto {
	private List<Z2JupiterTestIdentifierDto> roots = new ArrayList<>();
	private String id;
	
	public Z2JupiterTestPlanDto() {}
	
	public Z2JupiterTestPlanDto(String id, TestPlan testplan) {
		this.id = id;
		map(testplan, testplan.getRoots(),roots);
	}

	private void map(TestPlan testplan, Collection<TestIdentifier> source, Collection<Z2JupiterTestIdentifierDto> target) {
		source.forEach(ti->{
			Z2JupiterTestIdentifierDto zu5TiDto = new Z2JupiterTestIdentifierDto(ti);
			target.add(zu5TiDto);
			map(testplan,testplan.getChildren(ti.getUniqueId()),zu5TiDto.getChildren());
		});
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public List<Z2JupiterTestIdentifierDto> getRoots() {
		return roots;
	}

	public void setRoots(List<Z2JupiterTestIdentifierDto> roots) {
		this.roots = roots;
	}

}
