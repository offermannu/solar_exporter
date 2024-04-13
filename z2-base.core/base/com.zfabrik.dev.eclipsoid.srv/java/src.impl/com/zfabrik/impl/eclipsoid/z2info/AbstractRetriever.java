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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

public abstract class AbstractRetriever {

	private final static String DEFAULT_MIN_VERSION = "1.3.0";
	
	private HttpServletRequest request;
	private HttpServletResponse response;
	
	public AbstractRetriever getInstanceFor(HttpServletRequest req, HttpServletResponse resp) {
		AbstractRetriever result;
		try {
			result = this.getClass().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		result.request = req;
		result.response = resp;
		
		return result;
	}
	
	public boolean isProviderFor(String type) {
		return "all".equals(type);
	}
	
	public boolean supportsVersion(String clientVersion) {
		boolean result = DEFAULT_MIN_VERSION.compareTo(clientVersion) <= 0;
		return result;
	}
	
	public abstract void provideInfoInto(JSONObject result) throws IOException;

	public void setRequest(HttpServletRequest request) {
	}
	
	public HttpServletRequest getRequest() {
		return request;
	}
	
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	public HttpServletResponse getResponse() {
		return response;
	}
	
	protected boolean isSensitiveKey(String key) {
		key = key.toLowerCase();
		
		for (String sensitiveKey : SENSITIVE_PROPS) {
			if (key.contains(sensitiveKey)) return true; 
		}
		return false;
	}

	private final static String[] SENSITIVE_PROPS = {
		"passwd", "password", "pwd"
	};
}
