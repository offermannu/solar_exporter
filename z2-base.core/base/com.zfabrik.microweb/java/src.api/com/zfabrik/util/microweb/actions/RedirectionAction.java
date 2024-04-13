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

public class RedirectionAction implements IAction {
	private String target;
	private String match;
	
	public RedirectionAction(String match, String target) {
		this.target = target;
		this.match = match;
	}

	@Override
	public OutCome handle(ServletContext context, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		String ap = (String) req.getAttribute(MicroWebConstants.MICROWEB_APPLICATION_PATH);
		if ((this.match.equals(ap))) {
			return OutCome.redirect(target);
		}
		return null;
	}

}
