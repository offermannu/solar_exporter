/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.update.site;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UpdateSiteFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain fCahin) throws IOException, ServletException {

		HttpServletRequest hreq = (HttpServletRequest) req;
		HttpServletResponse hresp = (HttpServletResponse) resp;
		
		String path = hreq.getRequestURI();
		String redirectPath = path.replaceFirst("/eclipsoid/update", "/updates/eclipsoid");
		
		String qStr = hreq.getQueryString();
		if (qStr != null) {
			qStr = "?" + qStr;
		} else {
			qStr = "";
		}
		
		hresp.sendRedirect("https://www.z2-environment.net" + redirectPath + qStr);
	}

	@Override
	public void init(FilterConfig fc) throws ServletException {
	}

	@Override
	public void destroy() {
	}


}
