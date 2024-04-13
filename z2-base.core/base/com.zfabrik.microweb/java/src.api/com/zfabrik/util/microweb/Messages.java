/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class Messages {
	
	private static final String MESSAGES = "microweb_messages";

	@SuppressWarnings("unchecked")
	public static synchronized void add(HttpServletRequest req, short type, String message) {
		List<Message> l = (List<Message>) req.getAttribute(MESSAGES);
		if (l==null) {
			l = new ArrayList<Message>();
			req.setAttribute(MESSAGES,l);
		}
		l.add(new Message(type,message));
	}
}
