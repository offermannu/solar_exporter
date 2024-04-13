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

import com.zfabrik.util.runtime.Foundation;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

public class VersionRetriever extends AbstractRetriever {

	private static final String VERSION_KEY = "serverVersion";

	public String getJsonKey() {
		return VERSION_KEY;
	}
	
	public boolean isProviderFor(String type) {
		return true;
	}

	public boolean supportsVersion(String clientVersion) {
		return true;
	}
	
	public void provideInfoInto(JSONObject result) throws IOException {
		result.put(VERSION_KEY, Foundation.getProperties().getProperty(Foundation.Z2_VERSION, "n.a."));
	}

	public static String getClientVersion(HttpServletRequest req) {
		String result = req.getParameter("v");
		if (result == null || result.length() == 0) {
			result = "1.0.0";
		}
		return result;
	}
}
