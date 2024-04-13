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

import java.io.IOException;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import com.zfabrik.dev.z2jupiter.internal.engine.Z2JupiterTestEngine;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterLauncherDiscoveryRequestDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterSelectorDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestPlanDto;

/**
 * Action class for discovery and execution scenario.
 * <p>
 * After a discovery, the resulting server side test plan will be maintained in the
 * servlet's HTTP session and the session id serves as test plan id from the client.
 * <p>
 * That is, we serve one test plan per session and every discover/execute run creates a new session.
 * <p>
 * The client embeds a test plan representation that mimicks the server side
 * structure, as a substructure of the client side discovery - i.e. the 
 * parts discovered by the {@link Z2JupiterTestEngine}.
 * <p>
 * A subsequent execute refers to the test plan by id. 
 * <p>
 * During execution, all events are flushed into a JSON response stream
 * and may be picked up from the client to be translated into
 * events according to the client's representation of the
 * server side testplan.
 */
public class Z2JupiterDiscoverAndExecute extends Z2JupiterRequest {

	public Z2JupiterDiscoverAndExecute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super(request,response);
	}
	
	public void discover() {
		LOG.fine("Discovering for component "+this.componentName);

		// run local discovery
		runInServerContext(()-> {
			// turn into request dto
			Z2JupiterLauncherDiscoveryRequestDto z2JupiterLauncherDiscoveryRequestDto = getJsonPOST(Z2JupiterLauncherDiscoveryRequestDto.class);
			// build a request
			LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder.request().selectors(
				z2JupiterLauncherDiscoveryRequestDto.getSelectors().stream()
				.map(Z2JupiterSelectorDto::toSelector).collect(Collectors.toList())
			).build();
			// discover according to plan
			TestPlan plan = this.launcher().discover(discoveryRequest);
			// convert plan
			Z2JupiterTestPlanDto z2UnitTp = new Z2JupiterTestPlanDto(request.getSession(true).getId(), plan);
			// memorize original in session!
			request.getSession(false).setAttribute(TestPlan.class.getName(),plan);
			//
			LOG.fine("Discovered test plan "+z2UnitTp.getId()+" for component "+this.componentName);
			// write response
			prepareJsonResponse();
			sendJson(z2UnitTp);
		});	
	}
	

	public void execute() {
		runInServerContext(()->{
			HttpSession session = request.getSession(false);
			if (session==null) {
				throw new IllegalArgumentException("Lost session");
			}
			LOG.fine("Executing test plan "+session.getId()+" for component "+this.componentName);
			TestPlan tp = (TestPlan) session.getAttribute(TestPlan.class.getName());
			if (tp==null) {
				throw new IllegalArgumentException("No plan found for id "+session.getId());
			}
			// release
			session.removeAttribute(TestPlan.class.getName());
			runTestPlanAndStreamEventsToResponse(tp);
		});
	}

}