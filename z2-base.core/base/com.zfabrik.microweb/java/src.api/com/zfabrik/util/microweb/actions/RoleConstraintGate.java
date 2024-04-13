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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RoleConstraintGate implements IAction {
	private String roleName;
	private String loginAction;
	private IAction wrapped;
	
	public RoleConstraintGate(IAction wrapped,String roleName,String loginAction) {
		super();
		this.roleName = roleName;
		this.wrapped = wrapped;
		this.loginAction = loginAction;
	}

	public RoleConstraintGate(IAction wrapped,String roleName) {
		this(wrapped,roleName,null);
	}

	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (req.isUserInRole(this.roleName)) {
			return this.wrapped.handle(context, req, res);
		} else {
			if (this.loginAction!=null) {
				return OutCome.goTo(this.loginAction);
			} else {
				return OutCome.error(HttpServletResponse.SC_FORBIDDEN, "User not in appropriate role");
			}
		}
	}

}
