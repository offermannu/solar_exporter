/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.z2info;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;



public class InfoRetriever extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final static Logger logger = Logger.getLogger(InfoRetriever.class.getName());
	
	private final static AbstractRetriever[] INFO_RETRIEVERS = {
		new VersionRetriever(),
		new ComponentsRetriever(), 
		new ProjectsRetriever(), 
		new DependenciesRetriever(), 
		new RepositoriesRetriever(),
		new TemplatesRetriever(),
//		new ModuleLayoutRetriever(),
		new SystemPropertiesRetriever()
	};

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)	throws ServletException, IOException {

		logger.fine(req.getRequestURL().append('?').append(req.getQueryString()).toString());
		
		String info = req.getPathInfo();

		String action;
		if (info != null && info.startsWith("/")) { 
			action = info.substring(1);
		} else {
			action = "all";
		}

		JSONObject result = new JSONObject();

		String clientVersion = VersionRetriever.getClientVersion(req);
		
		try {
			for (AbstractRetriever infoRetriever : INFO_RETRIEVERS) {
				if (infoRetriever.isProviderFor(action) && infoRetriever.supportsVersion(clientVersion)) {
					infoRetriever.getInstanceFor(req, resp).provideInfoInto(result);
				}
			}
		} catch (IOException e) {
			result.put("error", "Failed to retrieve Status Information from z2-Server");
			throw e;
		}

		resp.setContentType("text/plain");
		resp.setCharacterEncoding("UTF-8");
		resp.getWriter().write(result.toString(2));
	}
}
