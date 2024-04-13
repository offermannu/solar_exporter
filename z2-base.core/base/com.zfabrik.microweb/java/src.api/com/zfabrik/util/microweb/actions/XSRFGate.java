/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.actions;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * In order to block Cross-Site Request Forgery attacks, we require that 
 * a request provides the current session id as a parameter by the name
 * jsessionid. 
 * This blocks attackers that send GETs or POSTs to  foreign domain from 
 * invoking an action.
 * @author hb
 *
 */
public class XSRFGate  implements IAction {
	public static final String[] XSRF_COOKIES = {"SSO_ID","JSESSIONID"};
	public static final String XSRF_PASSKEY =  "_XSRF_PASS_KEY";
	private IAction wrapped;
	
	public XSRFGate(IAction wrapped) {
		this.wrapped = wrapped;
	}

	
	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// check for internal include or dispatch.
		String contentType = req.getContentType();
		if ("post".equalsIgnoreCase(req.getMethod()) && (contentType==null || !contentType.toLowerCase().startsWith("multipart/"))) {
			// we let multipart messages pass. They should call {@link #check} on their own! 
			OutCome o = _check(req,XSRF_COOKIES,req.getParameter(XSRF_PASSKEY));
			if (o!=null) return o;
		}
		return this.wrapped.handle(context, req, res);
	}

	private final static Logger logger = Logger.getLogger(XSRFGate.class.getName());

	public static OutCome check(HttpServletRequest req, String passkey) {
		return _check(req, XSRF_COOKIES, passkey);
	}
	
	// --- helpers ---
	
	private static String _getCookie(HttpServletRequest req,String name) {
		Cookie[] cookies = req.getCookies();
		if (cookies!=null) {
			for (Cookie c : cookies) {
				if (name.equals(c.getName())) {
					return c.getValue();
				}
			}
		}
		return null;
	}

	private static OutCome _check(HttpServletRequest req, String[] cookieNames, String passkey) {
		if (passkey!=null) {
			for (String cn : cookieNames) {
				if (passkey.equals(_getCookie(req, cn))) {
					return null;
				}
			}
		}
		logger.warning("Incorrect _XSRF_PASS_KEY ("+passkey+") on protected request "+req.getRequestURI()+" from "+req.getRemoteAddr());
		return OutCome.error(HttpServletResponse.SC_FORBIDDEN);
	}
}
