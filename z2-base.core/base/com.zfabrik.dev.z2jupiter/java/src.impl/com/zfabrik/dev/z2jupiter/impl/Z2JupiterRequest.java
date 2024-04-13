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

import static com.zfabrik.dev.z2jupiter.internal.engine.Z2JupiterTestEngine.withoutTestEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient;
import com.zfabrik.dev.z2jupiter.internal.util.Json;
import com.zfabrik.util.threading.ThreadUtil;

/**
 * Generic abstract request handling for Z2Jupiter
 */
public abstract class Z2JupiterRequest {
	protected final static Logger LOG = Logger.getLogger(Z2JupiterDiscoverAndExecute.class.getName());

	protected HttpServletRequest request; 
	protected HttpServletResponse response;
	protected String componentName;
	protected ClassLoader loader;
	
	public Z2JupiterRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		super();
		this.request = request;
		this.response = response;
		this.requiresComponentName();
		this.loader = JavaComponentUtil.getPrivateLoaderOfJavaComponent(this.componentName, null);
		if (this.loader==null) {
			throw new IllegalArgumentException("Component "+this.componentName+" does not resolve to code");
		}
	}

	private void requiresComponentName() {
		this.componentName = StringUtils.trimToNull(request.getParameter(Z2JupiterClient.PARAM_COMPONENT_NAME));
		if (this.componentName==null) {
			throw new IllegalArgumentException("Missing parameter componentName");
		}
	}
	
	/**
	 * Run in context of test module with disabled Z2U
	 */
	protected void runInServerContext(Runnable run) {
		ThreadUtil.cleanContextRun(
			this.loader,
			()->withoutTestEngine(run)
		);
	}
	
	/**
	 * Retrieve JSON POST data 
	 */
	protected <T> T getJsonPOST(Class<T> clz) {
		requirePOST();
		String contentType = request.getContentType();
		if (!Z2JupiterClient.APPLICATION_JSON_CHARSET_UTF_8.equals(StringUtils.trimToEmpty(contentType.replace(" ", "").toLowerCase()))) {
			throw new IllegalArgumentException("Unsupported content type "+contentType);
		}
		return receiveJson(request ,clz);
	}

	/**
	 * Get JUnit launcher
	 */
	protected Launcher launcher() {
		return LauncherFactory.create();
	}

	
	private ObjectMapper om;
	
	/**
	 * Get Json Object Mapper
	 */
	protected ObjectMapper om() {
		if (om==null) {
			this.om = Json.om();
		}
		return this.om;
	}
	
	/**
	 * Json to output in response
	 */
	protected void sendJson(Object o) {
		try {
			String json = om().writeValueAsString(o);
			LOG.fine(()->"Sending \n"+json);
			IOUtils.write(json,  response.getOutputStream(), StandardCharsets.UTF_8);
			response.getOutputStream().flush();
		} catch (Exception e) {
			throw new IllegalArgumentException("Marshalling error "+e.getMessage(),e);
		}
	}

	/**
	 * Json from input in request
	 */
	protected <T> T receiveJson(HttpServletRequest request,  Class<T> clz) {
		try {
			String json = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
			LOG.fine(()->"Received \n"+json);
			return fromJson(json,clz);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unmarshalling error "+e.getMessage(),e);
		}
	}
	
	/**
	 * Json from string
	 */
	protected <T> T fromJson(String json, Class<T> clz) {
		try {
			return om().readValue(json, clz);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Unmarshalling error "+e.getMessage(),e);
		}
	}
	
	/**
	 * Prepare response for Json output
	 */
	protected HttpServletResponse prepareJsonResponse() {
		response.setContentType(Z2JupiterClient.APPLICATION_JSON_CHARSET_UTF_8);
		response.setBufferSize(0);
		return response;
	}
	
	/**
	 * Require HTTP method
	 */
	protected void requirePOST() {
		if (!Z2JupiterClient.HTTP_METHOD_POST.equals(request.getMethod())) {
			throw new IllegalArgumentException("Requiring POST for "+request.getRequestURI());
		}
	}

	/**
	 * Require HTTP method
	 */
	protected void requireGET() {
		if (!Z2JupiterClient.HTTP_METHOD_GET.equals(request.getMethod())) {
			throw new IllegalArgumentException("Requiring GET for "+request.getRequestURI());
		}
	}

	/**
	 * Run a given testplan, with disabled Z2JupiterEngine, and stream events back to response
	 * in JSON array
	 */
	protected void runTestPlanAndStreamEventsToResponse(TestPlan tp) {
		try (JsonGenerator json = om().getFactory().createGenerator(response.getOutputStream())) {
			json.writeStartArray();
			/*
			 * Execute with local launcher and send
			 * events back
			 */
			prepareJsonResponse();
			this.launcher().execute(
				tp, 
				// listener reporting back to 
				// the z2 jupiter client
				new Z2JupiterListenerAdapter(e->{
					try {
						if (LOG.isLoggable(Level.FINE)) {
							LOG.fine("Sending \n"+om().writeValueAsString(e));
						}
						om().writeValue(json, e);
					} catch (IOException ioe) {
						throw new IllegalArgumentException("Marshalling error "+ioe.getMessage(),ioe);
					}
				}),
				// listener enforcing dependencies
				new Z2DependencyPreparationListener()
			);
			json.writeEndArray();
		} catch (IOException ioe) {
			throw new IllegalArgumentException("Marshalling error "+ioe.getMessage(),ioe);
		}
	}

}