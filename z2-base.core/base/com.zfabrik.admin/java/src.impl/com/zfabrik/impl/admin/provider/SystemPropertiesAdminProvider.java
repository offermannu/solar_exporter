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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.management.home.IHomeMBeanServer;

public class SystemPropertiesAdminProvider implements IAdminProvider {
	IAdminProviderContext context;
	
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
	}

	@SuppressWarnings("unchecked")
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		b = new InfoBean("System Properties");
		TabularData props = (TabularData) mbs.getAttribute(ObjectName.getInstance("java.lang:type=Runtime"), "SystemProperties");
		Collection<CompositeData> data = (Collection<CompositeData>) props.values();
		for (CompositeData cd : data) {
			b.put(cd.get("key").toString(), cd.get("value").toString());
		}
		b.sortAttributesByKey();
		beans.add(b);
	}

	public void init(IAdminProviderContext context) {
		this.context =context; 
		context.setTabular(false);
	}

}
