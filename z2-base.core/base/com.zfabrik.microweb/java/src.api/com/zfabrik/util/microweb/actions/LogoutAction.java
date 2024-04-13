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
import java.security.Principal;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.zfabrik.util.microweb.actions.IAction;
import com.zfabrik.util.microweb.actions.OutCome;

/**
 * Handle logout
 * @author hb
 */
public class LogoutAction implements IAction {
	private IAction target;
	
	public LogoutAction(IAction target) {
		super();
		this.target = target;
	}

	public OutCome handle(ServletContext context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Principal un = request.getUserPrincipal();
		logger.info("User "+(un!=null?un.getName():"UNKNOWN")+" sent a logoff request to "+request.getRequestURI());
		HttpSession s = request.getSession(false);
		if (s != null) {
			s.invalidate();
		}
		return target.handle(context, request, response);
	}
	
	private final static Logger logger = Logger.getLogger(LogoutAction.class.getName());

}
