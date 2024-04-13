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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.management.ObjectName;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.impl.admin.web.vm.InfoBeanAction;
import com.zfabrik.management.home.IHomeMBeanServer;

public class LoggerAdminProvider implements IAdminProvider {
	IAdminProviderContext context;

	@Override
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
		String logger = params.get("logger");
		String level = params.get("level");
		
		if (logger!=null && level!=null) {
			mbs.invoke(ObjectName.getInstance("java.util.logging:type=Logging"), "setLoggerLevel", 
					new String[] { logger, level },	new String[] { String.class.getName(), String.class.getName() });
		}
	}

	@Override
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		// Logger
		// RM namespaces
		ObjectName on = ObjectName.getInstance("java.util.logging:type=Logging");
		String[] names = (String[]) mbs.getAttribute(on, "LoggerNames");

		for (String ln : names) {
			b = new InfoBean(ln);
			b.put("level", (String) mbs.invoke(on, "getLoggerLevel", new Object[] { ln }, new String[] { String.class.getName() }));

			b.add(new InfoBeanAction("setLogLevel", "FINEST").setParam("level", "FINEST").setParam("logger", ln));
			b.add(new InfoBeanAction("setLogLevel", "FINER").setParam("level", "FINER").setParam("logger", ln));
			b.add(new InfoBeanAction("setLogLevel", "FINE").setParam("level", "FINE").setParam("logger", ln));
			b.add(new InfoBeanAction("setLogLevel", "INFO").setParam("level", "INFO").setParam("logger", ln));
			b.add(new InfoBeanAction("setLogLevel", "WARNING").setParam("level", "WARNING").setParam("logger", ln));
			b.add(new InfoBeanAction("setLogLevel", "SEVERE").setParam("level", "SEVERE").setParam("logger", ln));
			beans.add(b);
		}
		Collections.sort(beans, new Comparator<InfoBean>() {
			public int compare(InfoBean o1, InfoBean o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
	}

	@Override
	public void init(IAdminProviderContext context) {
		this.context = context;
		context.setTabular(true);
	}

}
