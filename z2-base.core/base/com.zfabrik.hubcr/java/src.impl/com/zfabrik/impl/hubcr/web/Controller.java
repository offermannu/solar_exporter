/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.hubcr.web;

/*
 * z2-Environment
 * 
 * Copyright(c) 2010, 2011, 2012 ZFabrik Software KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.hubcr.RemoteComponentRepositoryDB;
import com.zfabrik.impl.hubcr.store.HubCRResource;
import com.zfabrik.impl.hubcr.store.HubCRManager;
import com.zfabrik.impl.hubcr.store.HubCRManager.Reporter;

public class Controller implements Filter {	
	@Override
	public void destroy() {}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void doFilter(ServletRequest sreq, ServletResponse sresp,FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) sreq;
		HttpServletResponse resp = (HttpServletResponse) sresp;

		if (toBeHandled(req) && (doPost(req,resp) || doGet(req,resp))) {
			goToView(req, resp);
		} else {
			chain.doFilter(req, resp);
		}
	}
	
	private boolean toBeHandled(HttpServletRequest req) {
		// filter paths
		String uri = req.getRequestURI().substring(req.getContextPath().length());
		return !uri.startsWith("/WEB-INF") && !uri.startsWith("/repo");
	}

	private boolean doPost(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!"post".equalsIgnoreCase(req.getMethod())) {
			return false;
		}
		
		if (req.getParameter("scan")!=null) {
			try {
				HubCRManager repo = HubCRResource.getManager();
				repo.scan(new Reporter() {
					public void log(Level level, String message) {
						if (level.intValue()>Level.FINE.intValue()) {
							addMessage(req, message);
						}
					}
				});
				RemoteComponentRepositoryDB db = repo.getDB();
				if (db==null) {
					throw new IllegalStateException("No DB generated");
				}
				addMessage(req, "Scan completed. Now holding "+repo.getDB().getComponents().size()+" components.");
			} catch (Exception e) {
				handleError(req, resp, e);
			}
		}
		return true;
	}
	
	private boolean doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!"get".equalsIgnoreCase(req.getMethod())) {
			return false;
		}
		return true;
	}
	
	private void handleError(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException,ServletException {
		@SuppressWarnings("unchecked")
		List<Exception> l = (List<Exception>) req.getAttribute("errors");
		if (l==null) {
			l = new LinkedList<Exception>();
			req.setAttribute("errors", l);
		}
		l.add(e);
	}

	private void addMessage(HttpServletRequest req,String message) {
		@SuppressWarnings("unchecked")
		List<String> l = (List<String>) req.getAttribute("messages");
		if (l==null) {
			l = new LinkedList<String>();
			req.setAttribute("messages", l);
		}
		l.add(message);
	}
	
	private void goToView(HttpServletRequest req, HttpServletResponse resp) throws IOException,ServletException {
		try {
			RemoteComponentRepositoryDB db = HubCRResource.getManager().getDB();
			if (db!=null) {
				req.setAttribute("revision", new Date(db.getRevision()));
			}
		} catch (Exception e) {
			handleError(req, resp, e);
		}
		req.getRequestDispatcher("/WEB-INF/index.jsp").forward(req, resp);
	}
}
