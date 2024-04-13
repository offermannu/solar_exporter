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
 * A chain action will call all actions in order until one of them returns
 * non-null;
 * 
 * @author hb
 * 
 */
public class ChainAction implements IAction {
	private IAction[] actions;

	public ChainAction(IAction... actions) {
		this.actions = actions;
	}

	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		OutCome o;
		for (IAction a : this.actions) {
			if ((o = a.handle(context, req, res)) != null) {
				return o;
			}
		}
		return null;
	}

}
