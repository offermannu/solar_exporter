/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.decoration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.util.microweb.MicroWebConstants;
import com.zfabrik.util.microweb.actions.MicroWebDispatcher;

/**
 * central decoration for this site/app
 * 
 * @author hb
 * 
 */

public class DecorationFilter implements Filter {
	private FilterConfig cfg;
	private String target;

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
		// we decorate at most once.
		boolean succeeded = false;
		try {
			HttpServletResponse response = (HttpServletResponse) res;
			HttpServletRequest request = (HttpServletRequest) req;
			MicroWebDispatcher mwd = new MicroWebDispatcher(this.cfg.getServletContext(), request,request.getRequestURI().substring(request.getContextPath().length()),null);
			if (mwd.decoratorForward(request, response,chain)) {
				RequestDispatcher rd = req.getRequestDispatcher(this.target);
				rd.forward(req, res);
			}
			succeeded = true;
		} finally {
			if (!succeeded && logger.isLoggable(Level.FINE)) {
				logger.warning("Failed to complete decoration on " + req.getAttribute(MicroWebConstants.MICROWEB_APPLICATION_PATH));
			}

		}
	}

	public void destroy() {
	}

	public synchronized void init(FilterConfig cfg) throws ServletException {
		this.cfg = cfg;
		this.target = this.cfg.getInitParameter("target");
		if (this.target == null) {
			throw new IllegalStateException("Must provider target parameter for decoration filter");
		}
	}

	final static Logger logger = Logger.getLogger(DecorationFilter.class.getName());
}
