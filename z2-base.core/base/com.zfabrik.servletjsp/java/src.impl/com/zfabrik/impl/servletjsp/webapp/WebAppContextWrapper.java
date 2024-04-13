/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.servletjsp.webapp;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.webapp.WebAppContext;

import com.zfabrik.servletjsp.Constants;
import com.zfabrik.work.ApplicationThreadPool;


/**
 * This wrapper of the WebAppContext makes sure that web app invocations are happening
 * with Z2 thread pool association.
 * <p>
 * We use the ScopedHandler approach of Jetty (since 7 or 8) to actually make sure 
 * we hook up within the session handler scope (which is required to make gateway work) 
 * 
 * @author hb
 *
 */
public class WebAppContextWrapper extends WebAppContext {
	private final static String REQ_COUNTED = WebAppContextWrapper.class.getName()+"/req";
	private final static String PAGE_COUNTED = WebAppContextWrapper.class.getName()+"/page";
	// NOTE: If you change this, change gateway's expiry retrieval
	private static final String GATEWAY_EXP = "com.zfabrik.gateway/exp";
	private static final String GATEWAY_SID = "com.zfabrik.gateway/sid";

	private long pageImpressions, requests;

	public synchronized long getRequests() {
		return requests;
	}
	
	public synchronized long getPageImpressions() {
		return pageImpressions;
	}
	
	// just a helper to forward an exception
	private static class RunnableException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public RunnableException(Exception e) {
			super(e);
		}
	}
	
	@Override
	public void doHandle(final String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		
		try {
			ApplicationThreadPool.instance().executeAs(new Runnable() {
				public void run() {
					HttpSession prevSession = request.getSession(false);
					String oldSid = (prevSession!=null? prevSession.getId():null);
					try {
						WebAppContextWrapper.super.doHandle(target, baseRequest, request, response);
					} catch (Exception ioe) {
						throw new RunnableException(ioe);
					} finally {
						synchronized (WebAppContextWrapper.this) {
							if (request.getAttribute(REQ_COUNTED)==null) {
								requests++;
								request.setAttribute(REQ_COUNTED, Boolean.TRUE);
							}
							if (request.getAttribute(Constants.ATTRIBUTE_IS_PAGE_IMPRESSION)!=null && request.getAttribute(PAGE_COUNTED)==null) {
								pageImpressions++;
								request.setAttribute(PAGE_COUNTED, Boolean.TRUE);
							}
							// keep track of max expiration (hack for gateway)
							// NOTE: If you change this, change gateway's expiry retrieval
							HttpSession s = request.getSession(false);
							if (s!=null) {
								Long exp = (Long) request.getAttribute(GATEWAY_EXP);
								exp = Math.max(exp==null? 0 : exp, System.currentTimeMillis()+s.getMaxInactiveInterval()*1000);
								request.setAttribute(GATEWAY_EXP, exp);
								request.setAttribute(GATEWAY_SID, s.getId());
							} else
							if (oldSid!=null) {
								// this means, we have a log out
								request.setAttribute(GATEWAY_EXP, 0l);
								request.setAttribute(GATEWAY_SID, oldSid);
							}
						}
					}
				}
			},true);
		} catch (RunnableException re) {
			// float exceptions!
			Throwable e = re.getCause();
			if (e instanceof ServletException) {
				throw new ServletException(e);
			}
			if (e instanceof IOException) {
				throw new IOException(e);
			}
			throw new RuntimeException(e);
		}
	}
	
}
