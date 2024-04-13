/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.validators;

import com.zfabrik.util.net.ParsedURL;

public class URLValidator {
	private final static String HTTP = "HTTP"; 
	private final static String HTTPS = "HTTPS"; 

	
	public static boolean isValidWebURL(String url) {
		ParsedURL ur = new ParsedURL(url);
		
		if (ur.getScheme()!=null && ur.getHost()!=null) {
			String s = ur.getScheme().toUpperCase();
			return HTTP.equals(s) || HTTPS.equals(s);
		}
		return false; 
	}
}
