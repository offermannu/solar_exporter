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

public class DecorationAction implements IAction {
	private OutCome o;
	
	public DecorationAction(String decorator, String target) {
		this(decorator,null,target);
	}
	
	public DecorationAction(String decorator, String prefix, String target) {
		this.o = OutCome.decorate(decorator, prefix, target);
	}
	
	public OutCome handle(ServletContext context, HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		return o;
	}
}
