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
import java.util.Set;

import javax.management.ObjectName;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.management.home.IHomeMBeanServer;
import static com.zfabrik.impl.admin.provider.OverviewAdminProvider.*;

public class WebAdminProvider implements IAdminProvider {
	
	private static final String CONTEXT_PATH = "ContextPath";
	private static final String MAX_INACTIVE_INTERVAL = "MaxInactiveInterval";
	private static final String MAX_SESSIONS = "MaxSessions";
	private static final String PAGE_IMPRESSIONS = "PageImpressions";

	private IAdminProviderContext context;
	
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
	}
	
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		Map<String, Object> al;
		Set<ObjectName> ons;
		// web apps
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.servletjsp.webapp.WebAppResource,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean(on.getKeyProperty("name"));
			al = context.getAsMap(mbs.getAttributes(on, new String[] { CONTEXT_PATH, MAX_INACTIVE_INTERVAL, MAX_SESSIONS, SESSIONS, REQUESTS,
					START_TIME, PAGE_IMPRESSIONS }));
			b.put(CONTEXT_PATH, context.getValue(al, CONTEXT_PATH));
			b.put(SESSIONS, context.getValue(al, SESSIONS) + " (max:" + context.getValue(al, MAX_SESSIONS) + ", maxinactive:"
					+ context.getValue(al, MAX_INACTIVE_INTERVAL) + "s)");
		
			b.put(REQUESTS, context.getValue(al, REQUESTS) + " (counted page impressions: " + context.getValue(al, PAGE_IMPRESSIONS) + ")");
			b.put(START_TIME, context.getValue(al, START_TIME));
			beans.add(b);
		}
		Collections.sort(beans, new Comparator<InfoBean>() {
			public int compare(InfoBean o1, InfoBean o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
	}

	public void init(IAdminProviderContext context) {
		this.context = context;
		context.setTabular(true);
	}

}
