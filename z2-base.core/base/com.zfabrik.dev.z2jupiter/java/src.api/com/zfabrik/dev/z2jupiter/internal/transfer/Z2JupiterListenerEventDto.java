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

import java.util.function.Function;

import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClientTestDescriptor;
import com.zfabrik.dev.z2jupiter.internal.util.UniqueIds;

/**
 * Payload of event transmission back to client and handling on client
 */
public class Z2JupiterListenerEventDto {

	/**
	 * Event type and dispatch to client side {@link TestExecutionListener}
	 */
	public static enum Type {
		testPlanExecutionStarted, testPlanExecutionFinished, dynamicTestRegistered, executionSkipped, executionStarted,
		executionFinished, reportingEntryPublished;
	}

	private Type type;
	private Z2JupiterTestResultDto testResult;
	private Z2JupiterTestIdentifierDto testIdentifier;
	private String reason;
	private Z2JupiterReportEntryDto reportEntry;
	
	private transient Z2JupiterClientTestDescriptor descriptor;
	private transient TestIdentifier identifier;
	// must be set for conversions on client!
	private transient Function<UniqueId,TestDescriptor> resolver; 
	
	public Z2JupiterListenerEventDto() {}

	/**
	 * Must be set on client, when processing to test identifier or testdescriptors
	 * @param clientTestPlan
	 * @return
	 */
	public Z2JupiterListenerEventDto withParentDescriptorResolver(Function<UniqueId,TestDescriptor> resolver) {
		this.resolver = resolver;
		return this;
	}
	
	/**
	 * Create JUnit5 descriptor given the event data
	 */
	public Z2JupiterClientTestDescriptor toJUnit5Descriptor() {
		return toJUnit5Descriptor(null);
	}
	
	/**
	 * Create JUnit5 descriptor given the event data and apply a prefix to the uniqueId
	 */
	public Z2JupiterClientTestDescriptor toJUnit5Descriptor(UniqueId prefix) {
		if (descriptor==null) {
			if (this.resolver==null) {
				throw new IllegalStateException("No resolver set!");
			}
			this.descriptor = new Z2JupiterClientTestDescriptor(this.testIdentifier, prefix);
			if (this.testIdentifier.getParentId()!=null) {
				// fix parent
				this.descriptor.setParent(
					this.resolver.apply(
						UniqueIds.concat(prefix, UniqueId.parse(this.testIdentifier.getParentId()))
					)
				);
			}
		}
		return this.descriptor;
	}

	/**
	 * Create JUnit5 identifier given the event data
	 */
	public TestIdentifier toJUnit5TestIdentifier() {
		if (identifier==null) {
			this.identifier = TestIdentifier.from(toJUnit5Descriptor());
		}
		return this.identifier;
	}
	
	public Type getType() {
		return type;
	}
	
	public void setType(Type type) {
		this.type = type;
	}
	
	public Z2JupiterListenerEventDto withType(Type type) {
		setType(type);
		return this;
	}
	
	public Z2JupiterTestResultDto getTestResult() {
		return testResult;
	}

	public void setTestResult(Z2JupiterTestResultDto testResult) {
		this.testResult = testResult;
	}

	public Z2JupiterListenerEventDto withTestResult(Z2JupiterTestResultDto testResult) {
		setTestResult(testResult);
		return this;
	}

	public Z2JupiterTestIdentifierDto getTestIdentifier() {
		return testIdentifier;
	}
	
	public void setTestIdentifier(Z2JupiterTestIdentifierDto testIdentifier) {
		this.testIdentifier = testIdentifier;
	}
	
	public Z2JupiterListenerEventDto withTestIdentifier(Z2JupiterTestIdentifierDto testIdentifier) {
		setTestIdentifier(testIdentifier);
		return this;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
	
	public Z2JupiterListenerEventDto withReason(String reason) {
		setReason(reason);
		return this;
	}
	
	public Z2JupiterReportEntryDto getReportEntry() {
		return reportEntry;
	}
	
	public void setReportEntry(Z2JupiterReportEntryDto reportEntry) {
		this.reportEntry = reportEntry;
	}

	public Z2JupiterListenerEventDto withReportEntry(Z2JupiterReportEntryDto reportEntry) {
		this.setReportEntry(reportEntry);
		return this;
	}
}
