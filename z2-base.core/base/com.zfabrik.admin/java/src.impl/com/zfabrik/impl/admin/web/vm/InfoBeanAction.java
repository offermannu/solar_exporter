/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.web.vm;

import java.util.HashMap;
import java.util.Map;

public class InfoBeanAction {
	private String name, text;
	private Map<String,String> params;
	
	public InfoBeanAction(String name,String text) {
		this.name = name;
		this.text = text;
	}
	
	public String getName() {
		return name;
	}
	public String getText() {
		return text;
	}
 
	public Map<String, String> getParams() {
		if (this.params==null) {
			this.params = new HashMap<String, String>();
		}
		return params;
	}
	
	public InfoBeanAction setParam(String n,String v) {
		getParams().put(n,v);
		return this;
	}
	
}
