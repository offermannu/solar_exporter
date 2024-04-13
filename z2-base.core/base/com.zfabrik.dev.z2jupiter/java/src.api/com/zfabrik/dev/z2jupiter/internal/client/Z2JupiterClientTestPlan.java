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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.platform.commons.PreconditionViolationException;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestPlanDto;

/**
 * Client side {@link TestPlan} implementation derived from a server side discovered {@link TestPlan} conveyed
 * to the client as {@link Z2JupiterTestPlanDto}.
 */
public class Z2JupiterClientTestPlan extends TestPlan {
	private TestPlan delegate;
	private String id; 
	// we track all descriptors, as we need to resolve parent descriptors by id in dynamic 
	// test registration
	private Map<UniqueId,TestDescriptor> descriptors = new HashMap<>();
	
	public static Z2JupiterClientTestPlan fromDto(Z2JupiterTestPlanDto tp) {
		List<TestDescriptor> ds = tp.getRoots().stream().map(Z2JupiterClientTestDescriptor::new).collect(Collectors.toList());
		TestPlan delegate = TestPlan.from(ds);
		return new Z2JupiterClientTestPlan(tp.getId(), delegate, ds);
	}

	private Z2JupiterClientTestPlan(String id, TestPlan delegate, List<TestDescriptor> ds) {
		super(delegate.containsTests());
		this.id = id;
		this.delegate = delegate;
		ds.forEach(this::track);
	}
	
	private void track(TestDescriptor d) {
		descriptors.put(d.getUniqueId(), d);
		d.getChildren().forEach(this::track);
	}
	
	public String getId() {
		return id;
	}
	
	/**
	 * Add a client test descriptor to the plan - during dynamic test registration.
	 * We add by descriptor so that we can track (which we need for parent test descriptor
	 * resolution so... that we can use {@link TestIdentifier#from(TestDescriptor)} and have a 
	 * good hierarchy (and there seems to be no other way to fix the parent of a {@link TestIdentifier}). 
	 */
	public void add(Z2JupiterClientTestDescriptor d) {
		track(d);
		add(TestIdentifier.from(d));
	}

	public TestDescriptor findDescriptor(String id) {
		if (id!=null) {
			return this.descriptors.get(UniqueId.parse(id));
		}
		return null;
	}
	
	@SuppressWarnings("deprecation")
	public void add(TestIdentifier testIdentifier) {
		delegate.add(testIdentifier);
	}


	public Set<TestIdentifier> getRoots() {
		return delegate.getRoots();
	}


	public Optional<TestIdentifier> getParent(TestIdentifier child) {
		return delegate.getParent(child);
	}


	public Set<TestIdentifier> getChildren(TestIdentifier parent) {
		return delegate.getChildren(parent);
	}


	public Set<TestIdentifier> getChildren(String parentId) {
		return delegate.getChildren(parentId);
	}


	public TestIdentifier getTestIdentifier(String uniqueId) throws PreconditionViolationException {
		return delegate.getTestIdentifier(uniqueId);
	}


	public long countTestIdentifiers(Predicate<? super TestIdentifier> predicate) {
		return delegate.countTestIdentifiers(predicate);
	}


	public Set<TestIdentifier> getDescendants(TestIdentifier parent) {
		return delegate.getDescendants(parent);
	}


	public boolean containsTests() {
		return delegate.containsTests();
	}
}
