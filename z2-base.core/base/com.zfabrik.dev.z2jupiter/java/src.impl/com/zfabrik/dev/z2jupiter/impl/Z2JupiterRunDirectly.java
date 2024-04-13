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
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

/**
 * Direct execution of a test by component, class, and optionally method.
 * <p>
 * Result of the execution will be streamed to the client just as for {@link Z2JupiterDiscoverAndExecute#execute()}
 */
public class Z2JupiterRunDirectly extends Z2JupiterRequest {
	private final static Logger LOG = Logger.getLogger(Z2JupiterRunDirectly.class.getName());
	private static final String PARAM_TEST_CLASS  = "class";
	private static final String PARAM_TEST_METHOD = "method";

	private String testClass;
	private String testMethod;
	
	public Z2JupiterRunDirectly(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super(request, response);
		requiresTestClass(request);
		this.testMethod = StringUtils.trimToNull(request.getParameter(PARAM_TEST_METHOD));
	}

	private void requiresTestClass(HttpServletRequest request) {
		this.testClass = StringUtils.trimToNull(request.getParameter(PARAM_TEST_CLASS));
		if (this.testClass==null) {
			throw new IllegalArgumentException("Missing parameter componentName");
		}
	}

	public void run() {
		runInServerContext(()->{
			LOG.info("Running "+(testMethod==null?"":" test method "+testMethod+" of ")+"test class "+testClass+" of component "+this.componentName);
			
			DiscoverySelector selector;
			if (this.testMethod!=null) {
				// method of class
				selector = DiscoverySelectors.selectMethod(this.testClass, this.testMethod);
			} else {
				// whole class
				selector = DiscoverySelectors.selectClass(this.testClass);
			}
			// create discovery request
			LauncherDiscoveryRequest discoveryRequest = LauncherDiscoveryRequestBuilder
				.request()
				.selectors(selector)
				.build();
			// prepare the response
			prepareJsonResponse();
			// let it happen!
			runTestPlanAndStreamEventsToResponse(this.launcher().discover(discoveryRequest));
		});
	}

}
