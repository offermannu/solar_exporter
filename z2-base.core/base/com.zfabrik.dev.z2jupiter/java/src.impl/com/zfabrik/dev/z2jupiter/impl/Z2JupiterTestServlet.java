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

import static com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient.CALL_DISCOVER;
import static com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient.CALL_EXECUTE;
import static com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient.CALL_RUN;
import static org.apache.commons.lang3.StringUtils.stripEnd;
import static org.apache.commons.lang3.StringUtils.stripStart;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient;

/**
 * Servlet handling requests from {@link Z2JupiterClient}. In addition to
 * discovery and execution requests, tests may also be invoked by simple
 * GET requests.
 */
@WebServlet(urlPatterns = {"/*"})
public class Z2JupiterTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger LOG = Logger.getLogger(Z2JupiterTestServlet.class.getName());

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			// determine method to invoke from path
			String call = stripEnd(stripStart(req.getRequestURI().substring(req.getContextPath().length()),"/"),"/");
			switch (call) {
			case CALL_DISCOVER: 
				new Z2JupiterDiscoverAndExecute(req,resp).discover();
				break;
			case CALL_EXECUTE: 
				new Z2JupiterDiscoverAndExecute(req,resp).execute();
				break;
			case CALL_RUN: 
				new Z2JupiterRunDirectly(req,resp).run();
				break;
			default:
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported call "+call);
			}
		} catch (IllegalArgumentException iae) {
			LOG.log(Level.WARNING,"Bad Z2 Jupiter Request",iae);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, iae.getMessage());
		}
	}

}
