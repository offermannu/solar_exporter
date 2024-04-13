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

/**
 * For this action will make the microwebfilter ignore the request and 
 * continue with the filter chain.
 */
public class PassThroughAction implements IAction {
	
	public PassThroughAction() {
	}


	public OutCome handle(ServletContext context, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		return OutCome.passThrough();
	}

}
