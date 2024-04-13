/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.provider;

import java.util.Date;
import java.util.Map;

import javax.management.AttributeList;

public interface IAdminProviderContext {
	
	void setTabular(boolean tabular); 
	
	String getValue(Map<String, Object> map, String key);
	
	Map<String, Object> getAsMap(AttributeList al);
	
	String format(Date date);

	Number getNumber(Map<String, Object> map, String key);
}
