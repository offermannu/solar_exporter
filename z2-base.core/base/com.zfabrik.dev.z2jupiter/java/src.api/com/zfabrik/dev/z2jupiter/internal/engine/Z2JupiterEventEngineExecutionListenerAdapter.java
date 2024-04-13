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

import java.util.function.Function;
import java.util.logging.Logger;

import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;

import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClientTestDescriptor;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterListenerEventDto;

/**
 * Translating {@link Z2JupiterListenerEventDto} to {@link EngineExecutionListener} invocation.
 * 
 * Also, as we have nested test plans on the client, optionally apply a {@link UniqueId} prefix.
 * 
 * Furthermore, when receiving a dynamic test registration, the test is added to the parent
 * descriptor.
 */
public class Z2JupiterEventEngineExecutionListenerAdapter {
	private final static Logger LOG = Z2JupiterTestEngine.LOG;
	private EngineExecutionListener listener;
	private UniqueId prefix;
	private Function<UniqueId,TestDescriptor> resolver;
	
	public Z2JupiterEventEngineExecutionListenerAdapter(UniqueId prefix, EngineExecutionListener listener, Function<UniqueId,TestDescriptor> resolver) {
		super();
		this.listener=listener;
		this.prefix = prefix;
		this.resolver = resolver;
	}
	
	private Z2JupiterClientTestDescriptor getDescriptor(Z2JupiterListenerEventDto e) {
		return e.withParentDescriptorResolver(resolver).toJUnit5Descriptor(this.prefix);
	}
	
	private boolean isKnown(Z2JupiterClientTestDescriptor d) {
		return this.resolver.apply(d.getUniqueId()) != null;
	}
	
	public void dispatch(Z2JupiterListenerEventDto e) {
		
		switch (e.getType()) {
		case testPlanExecutionStarted:
			break;
		case testPlanExecutionFinished:
			break;
		case dynamicTestRegistered: {
				Z2JupiterClientTestDescriptor d = getDescriptor(e);
				listener.dynamicTestRegistered(d);
				LOG.fine("Reporting "+e.getType().name()+" to "+d.getUniqueId());
				// register with parent so it can receive further events
				d.getParent().get().addChild(d);
			}
			break;
		case executionSkipped: {
				Z2JupiterClientTestDescriptor d = getDescriptor(e);
				if (isKnown(d)) {
					LOG.fine("Reporting "+e.getType().name()+" to "+d.getUniqueId());
					listener.executionSkipped(d, e.getReason());
				}
			}
			break;
		case executionStarted: {
				Z2JupiterClientTestDescriptor d = getDescriptor(e);
				if (isKnown(d)) {
					LOG.fine("Reporting "+e.getType().name()+" to "+d.getUniqueId());
					listener.executionStarted(d);
				}
			}
			break;
		case executionFinished: {
				Z2JupiterClientTestDescriptor d = getDescriptor(e);
				if (isKnown(d)) {
					LOG.fine("Reporting "+e.getType().name()+" to "+d.getUniqueId());
					listener.executionFinished(d, e.getTestResult().toTestExecutionResult());
				}
			}
			break;
		case reportingEntryPublished: {
				Z2JupiterClientTestDescriptor d = getDescriptor(e);
				if (isKnown(d)) {
					LOG.fine("Reporting "+e.getType().name()+" to "+d.getUniqueId());
					listener.reportingEntryPublished(d, e.getReportEntry().toReportEntry());
				}
			}
			break;
		default:
			throw new IllegalArgumentException();
		}
	}



}
