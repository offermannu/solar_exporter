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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.impl.admin.web.vm.InfoBeanAction;
import com.zfabrik.management.home.IHomeMBeanServer;

public class TimersAdminProvider implements IAdminProvider {
	private static final String TIMERS = "Timers";
	
	IAdminProviderContext context;

	@Override
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
		String timer = params.get("timer");
		String component = params.get("component");
		if (timer!=null && component!=null) {
			ObjectName on = ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.timer.DispatcherResource,name="+component);
			if (params.containsKey("kickTimer")) {
				mbs.invoke(on,"kickTimer",new String[]{timer},new String[]{String.class.getName()});
			}
			if (params.containsKey("cancelTimer")) {
				mbs.invoke(on,"cancelTimer",new String[]{timer},new String[]{String.class.getName()});
			}
		}
	}

	@Override
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		Map<String, Object> al;
		Set<ObjectName> ons;
		// web apps
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.timer.DispatcherResource,*"), null);
		for (ObjectName on : ons) {
			al = context.getAsMap(mbs.getAttributes(on, new String[] {TIMERS}));
			
			CompositeData[] cds = (CompositeData[]) al.get(TIMERS);
			if (cds!=null) {
				for (CompositeData cd : cds) {
					b = new InfoBean(on.getKeyProperty("name")+": " +cd.get("id"));
					
					b.put("component", (String) cd.get("component")); 
					b.put("expiration", context.format(new Date((Long) cd.get("expiration")))); 
					b.put("interval", Number.class.cast(cd.get("interval")).toString()); 
					b.put("attempts", Number.class.cast(cd.get("attempts")).toString()); 
					b.put("maxAttempts", Number.class.cast(cd.get("maxAttempts")).toString()); 
					b.put("error", (String) cd.get("error")); 
					b.put("failed", Boolean.toString((Boolean) cd.get("failed"))); 
					b.put("lockedUntil", context.format(new Date(Number.class.cast(cd.get("lockedUntil")).longValue()))); 
					
					b.add(new InfoBeanAction("kickTimer", "kick")
						.setParam("timer",(String)cd.get("id"))
						.setParam("component",on.getKeyProperty("name"))
					);
					b.add(new InfoBeanAction("cancelTimer", "cancel")
						.setParam("timer",(String)cd.get("id"))
						.setParam("component",on.getKeyProperty("name"))
					);
					
					beans.add(b);
				}
			}
		}		
	}

	@Override
	public void init(IAdminProviderContext context) {
		this.context = context;
	}

}
