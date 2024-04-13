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

import com.zfabrik.util.microweb.MicroWebConstants;

/**
 * to wrap other actions that should only be effective upon an exact path match
 * @author hb
 *
 */
public class ExactPathGate implements IAction {
	private String path;
	private IAction wrapped;
	
	
	public ExactPathGate(IAction wrapped,String path) {
		super();
		this.path = path;
		this.wrapped = wrapped;
	}


	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		if (this.path.equals(req.getAttribute(MicroWebConstants.MICROWEB_APPLICATION_PATH))) {
			return this.wrapped.handle(context, req, res);
		}
		return null;
	}

}
