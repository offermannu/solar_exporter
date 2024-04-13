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

import java.util.List;
import java.util.Map;

public class InfoTableRow {
	private Map<String, String> data;
	private List<InfoBeanAction> actions;
	
	public InfoTableRow(Map<String, String> data, List<InfoBeanAction> actions) {
		this.data=data;
		this.actions=actions;
	}
	
	public Map<String, String> getData() {
		return data;
	}
	
	public List<InfoBeanAction> getActions() {
		return actions;
	}

}
