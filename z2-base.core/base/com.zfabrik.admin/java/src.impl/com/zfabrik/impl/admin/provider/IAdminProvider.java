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

import java.util.List;
import java.util.Map;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.management.home.IHomeMBeanServer;

public interface IAdminProvider {
	
	void init(IAdminProviderContext context);
	
	void action(IHomeMBeanServer mbs, Map<String,String> params, List<String> messages) throws Exception;
	
	void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception;
	
}
