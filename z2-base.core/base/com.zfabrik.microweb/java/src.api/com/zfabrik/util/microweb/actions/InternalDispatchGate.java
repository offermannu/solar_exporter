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

public class InternalDispatchGate implements IAction {
	private IAction wrapped;
	
	public InternalDispatchGate(IAction wrapped) {
		this.wrapped = wrapped;
	}
	
	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		// check for internal include or dispatch.
		if (	req.getAttribute("javax.servlet.forward.request_uri")!=null ||
				req.getAttribute(MicroWebConstants.MICROWEB_INTERNAL_DISPATCH)!=null ||
				req.getAttribute("javax.servlet.include.request_uri")!=null) {
			return wrapped.handle(context, req, res);
		} else {
			return OutCome.error(HttpServletResponse.SC_FORBIDDEN);
		}
	}

}
