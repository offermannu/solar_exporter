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

import static com.zfabrik.util.microweb.MicroWebConstants.MICROWEB_CONTEXT_PATH;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.util.microweb.MicroWebConstants;
import com.zfabrik.util.microweb.decoration.ResponseWrapper;

public class MicroWebDispatcher {
	private ServletContext targetContext;
	private String uri;
	private String prefix;

	public MicroWebDispatcher(ServletContext context, HttpServletRequest request,String target, String prefix) {
		// if there is a prefix we go to <target>/{<uri>\\<prefix>}
		// i.e. we translate a namespace!
		// if there is no prefix we go to <target>
		// remove the prefix part first ( so, <prefix>/this/that becomes /this/that)
		this.uri = (String) request.getAttribute(MicroWebConstants.MICROWEB_APPLICATION_PATH);
		if (prefix!=null) {
			uri = uri.substring(prefix.length());
			if (uri.length()==0) { uri="/"; }
		}
		this.targetContext = context;
		if (!target.startsWith("/")) {
			uri = uri+"/"+target;
		} else {
			if (target.startsWith("//")) {
				// means different context.
				int p = target.indexOf('/',2);
				String ctx = (p<0? target.substring(1):target.substring(1,p));
				target = (p<0?"/":target.substring(p));
				this.targetContext = context.getContext(ctx);
				if (this.targetContext ==null) {
					throw new IllegalArgumentException("Failed to find target context ("+ctx+")");
				}
			}
			if (prefix!=null) {
				uri = ("/".equals(target)? uri:target+uri);
			} else { 
				uri = target;
			}
		}
		this.prefix = prefix;
	}

	public void forward(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		RequestDispatcher rd=  this.targetContext.getRequestDispatcher(this.uri);
		String prf = (String) request.getAttribute(MICROWEB_CONTEXT_PATH);
		try {
			if (this.prefix!=null) {
				request.setAttribute(MICROWEB_CONTEXT_PATH, (prf==null? this.prefix:prf+this.prefix));
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Forwarding to context="+this.targetContext.getContextPath()+", uri="+uri);
			}
			rd.forward(request,response);
		} finally {
			request.setAttribute(MICROWEB_CONTEXT_PATH, prf);
		}

	}

	public void include(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		RequestDispatcher rd=  this.targetContext.getRequestDispatcher(this.uri);
		String prf = (String) request.getAttribute(MICROWEB_CONTEXT_PATH);
		try {
			if (this.prefix!=null) {
				request.setAttribute(MICROWEB_CONTEXT_PATH, (prf==null? this.prefix:prf+this.prefix));
			}
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("Including context="+this.targetContext.getContextPath()+", uri="+uri);
			}
			rd.include(request,response);
		} finally {
			request.setAttribute(MICROWEB_CONTEXT_PATH, prf);
		}
	}

	public boolean decoratorForward(HttpServletRequest request,HttpServletResponse response)  throws IOException, ServletException {
		return decoratorForward(request, response, null);
	}

	public boolean decoratorForward(HttpServletRequest request,HttpServletResponse response, FilterChain chain)  throws IOException, ServletException {
		String k = this.getClass().getName()+this.getAbsoluteTarget();
		boolean decorated = (request.getAttribute(k)!=null);
		if (!decorated) {
			request.setAttribute(k,Boolean.TRUE);
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("MicroWeb.Decoration: Preparing decoration of " + getAbsoluteTarget());
			}
			// process the actual request first
			ResponseWrapper w = new ResponseWrapper(response);
			if (chain==null) {
				this.forward(request, w);
			} else {
				chain.doFilter(request,w);
			}
			if (w.isValid()) {
				if (logger.isLoggable(Level.FINE)) {
					logger.fine("MicroWeb.Decoration: decoration enabled of " + getAbsoluteTarget());
				}
				request.setAttribute("content", w.getContent());
				return true;
			}
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("MicroWeb.Decoration: not decorating because of nesting of " + getAbsoluteTarget());
		}
		return false;
	}
	
	private final static Logger logger = Logger.getLogger(MicroWebDispatcher.class.getName());

	public String getAbsoluteTarget() {
		return this.targetContext.getContextPath()+this.uri;
	}
}
