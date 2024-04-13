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

import com.zfabrik.servletjsp.Constants;

/**
 * just sets the page impression flag
 * 
 * @author hb
 * 
 */
public class PageViewAction implements IAction {
	private IAction wrapped;

	public PageViewAction(IAction wrapped) {
		super();
		this.wrapped = wrapped;
	}

	public OutCome handle(ServletContext context, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		req.setAttribute(Constants.ATTRIBUTE_IS_PAGE_IMPRESSION, Boolean.TRUE);
		return this.wrapped.handle(context, req, res);
	}

}
