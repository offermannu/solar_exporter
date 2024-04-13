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
 * given a URL of the form [prefix][remainder] we forward to [target][remainder]
 * @author hb
 *
 */
public class IncludeAction implements IAction {
	private String target;
	private String prefix;
	
	public IncludeAction(String target) {
		this.target = target;
	}

	/**
	 * An include action may work on a prefix basis. That is, instead of serving exactly one target,
	 * it serves a whole namespace. It does so by taking off the context path and the prefix and appends the remainder
	 * to the target path (that should, as always, have a leading slash and no trailing slashes).
	 * @param prefix
	 * @param target
	 */
	public IncludeAction(String prefix, String target) {
		this.target = target;
		this.prefix = prefix;
	}

	@Override
	public OutCome handle(ServletContext context, HttpServletRequest req,
			HttpServletResponse res) throws ServletException, IOException {
		return OutCome.include(this.prefix,target);
	}

}
