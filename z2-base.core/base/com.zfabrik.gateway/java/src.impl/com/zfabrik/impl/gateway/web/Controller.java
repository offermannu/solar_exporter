/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway.web;

import java.io.IOException;

import javax.management.ObjectName;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.zfabrik.gateway.GatewayFactory;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.util.runtime.Foundation;

/**
 * Controller filter for the Gateway management Web app.
 */
public class Controller implements Filter {

	@Override
	public void destroy() {}
	
	@Override
	public void init(FilterConfig arg0) throws ServletException {}
	
	@Override
	public void doFilter(ServletRequest sreq, ServletResponse sresp,FilterChain chain) throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) sreq;
		HttpServletResponse resp = (HttpServletResponse) sresp;

		// park id on request for view
		HttpSession s = req.getSession(false);
		if (s!=null) {
			req.setAttribute("sessionId", s.getId());
		}
		
		if (
			!toBeHandled(req) || (
				!logoff(req,resp) && 
				!doPost(req,resp) &&
				!doGet(req,resp)
			)
		) {
			chain.doFilter(req, resp);
		}
	}
	
	private boolean toBeHandled(HttpServletRequest req) {
		// filter paths
		String uri = req.getRequestURI();
		return !uri.startsWith("/WEB-INF");
	}

	private boolean doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!"post".equalsIgnoreCase(req.getMethod())) {
			return false;
		}
		String worker = req.getParameter("worker");
		if (worker!=null) {
			try {
				IHomeMBeanServer mbs = IHomeJMXClient.INSTANCE.getRemoteMBeanServer(null);
				ObjectName on = ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.workers.home.WorkerProcessResource,name="+worker);
				mbs.invoke(on, "detach", new Object[0],new String[0]);
				on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
				mbs.invoke(on, "asynchronize", new Object[0], new String[0]);
				reset(req,resp);
			} catch (Exception e) {
				handleError(req,resp,e);
			}
		}
		return true;
	}
	
	private boolean doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (!"get".equalsIgnoreCase(req.getMethod())) {
			return false;
		}
		try {
			String worker = getTargetWorkerName();
			if (worker!=null) {
				// go to home and fetch state
				IHomeMBeanServer mbs = IHomeJMXClient.INSTANCE.getRemoteMBeanServer(null);
				ObjectName on = ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.workers.home.WorkerProcessResource,name="+worker);
			    Short state = (Short) mbs.getAttribute(on, "State");
			    req.setAttribute("worker", worker);
			    req.setAttribute("state", state);
			    req.setAttribute("stateString", getStateString(state));
			    goToView(req, resp);
			}
		} catch (Exception e) {
			handleError(req, resp, e);
		}
		return true;
	}
	
	private String getTargetWorkerName() {
		return GatewayFactory.getInfo().getTargetWorkerName();
	}
	
	private boolean logoff(HttpServletRequest req, HttpServletResponse resp)  throws IOException,ServletException {
		if (req.getParameter("logoff")!=null) {
			req.getSession().invalidate();
			reset(req, resp);
			return true;
		}
		return false;
	}

	private String getStateString(Short state) {
		if (state!=null && state>=0 && state<=4) {
			return new String[]{ "NOT_STARTED","STARTING","STARTED","DETACHED","STOPPING"}[state];
		}
		return "n.a.";
	}

	private void handleError(HttpServletRequest req, HttpServletResponse resp, Exception e) throws IOException,ServletException {
		req.setAttribute("error", e);
		goToView(req, resp);
	}

	private void reset(HttpServletRequest req, HttpServletResponse resp) throws IOException,ServletException {
		resp.sendRedirect(req.getContextPath()+"/");
	}

	private void goToView(HttpServletRequest req, HttpServletResponse resp) throws IOException,ServletException {
		req.setAttribute("currentWorker",Foundation.getProperties().getProperty(Foundation.PROCESS_WORKER));		
		req.getRequestDispatcher("/WEB-INF/index.jsp").forward(req, resp);
	}
}
