/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.impl;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterListenerEventDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterListenerEventDto.Type;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterReportEntryDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestIdentifierDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestResultDto;

/**
 * Test listener that translates Junit5 {@link TestExecutionListener} calls into {@link Z2JupiterListenerEventDto} 
 * 
 * This is used to prepare transfer of events to the client
 */
public class Z2JupiterListenerAdapter implements TestExecutionListener {
	private final static Logger LOG = Logger.getLogger(Z2JupiterListenerAdapter.class.getName());
	private final Consumer<Z2JupiterListenerEventDto> listener;

	public Z2JupiterListenerAdapter(Consumer<Z2JupiterListenerEventDto> listener) {
		this.listener = listener;
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.dynamicTestRegistered)
			.withTestIdentifier(new Z2JupiterTestIdentifierDto(testIdentifier))
		);
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		if (testExecutionResult.getStatus()==Status.FAILED) {
			LOG.log(
				Level.SEVERE,
				"Test execution "+testIdentifier+" failed",
				testExecutionResult.getThrowable().orElse(null)
			);
		}
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.executionFinished)
			.withTestIdentifier(new Z2JupiterTestIdentifierDto(testIdentifier))
			.withTestResult(new Z2JupiterTestResultDto(testExecutionResult))
		);
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.executionSkipped)
			.withTestIdentifier(new Z2JupiterTestIdentifierDto(testIdentifier))
			.withReason(reason)
		);
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.testPlanExecutionFinished)
		);
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.reportingEntryPublished)
			.withTestIdentifier(new Z2JupiterTestIdentifierDto(testIdentifier))
			.withReportEntry(new Z2JupiterReportEntryDto(entry))
		);
	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.testPlanExecutionStarted)
		);
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		listener.accept(
			new Z2JupiterListenerEventDto()
			.withType(Type.executionStarted)
			.withTestIdentifier(new Z2JupiterTestIdentifierDto(testIdentifier))
		);
	}
}