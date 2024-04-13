/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.web;

import java.util.HashMap;
import java.util.Map;

import com.zfabrik.impl.admin.web.vm.Admin;
import com.zfabrik.impl.admin.web.vm.Log;
import com.zfabrik.util.microweb.actions.BeanDerivedAction;
import com.zfabrik.util.microweb.actions.IAction;
import com.zfabrik.util.microweb.actions.IActionProvider;
import com.zfabrik.util.microweb.actions.RedirectionAction;
import com.zfabrik.util.microweb.actions.RoleConstraintGate;

public class Actions implements IActionProvider {
	private Map<String,IAction> actions = new HashMap<String, IAction>();
	
	public Actions() {
		actions.put("/", new RedirectionAction("/","/admin"));
		actions.put("/admin", new RoleConstraintGate(new BeanDerivedAction(Admin.class),"admin"));
		actions.put("/log", new RoleConstraintGate(new BeanDerivedAction(Log.class),"admin"));
	}

	public IAction getAction(String uri) {
		return actions.get(uri);
	}


}
