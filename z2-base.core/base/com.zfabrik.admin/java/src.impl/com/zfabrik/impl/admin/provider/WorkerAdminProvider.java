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
import com.zfabrik.impl.admin.web.vm.InfoBeanAction;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.workers.home.IWorkerProcess;

public class WorkerAdminProvider implements IAdminProvider {
	private static final String LAST_ERROR = "LastError";
	private static final String DETACH_TIME = "DetachTime";
	private static final String LAST_SUCCESSFUL_START = "LastSuccessfulStart";
	private static final String STATE = "State";
	private static final String PID = "PID";
	private IAdminProviderContext context;
	
	@Override
	public void init(IAdminProviderContext context) {
		this.context = context;
		context.setTabular(true);
	}

	@Override
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
		String name = params.get("name");
		String action = params.get("action");
		if (name!=null && action!=null) {
			ObjectName on = ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.workers.home.WorkerProcessResource,name="+name);
			mbs.invoke(on, action, new Object[0],new String[0]);
			messages.add("Invoked \""+action+"\" on worker "+name);
		}		
	}

	@Override
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		Map<String, Object> al;
		Set<ObjectName> ons;
		// worker processes
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.workers.home.WorkerProcessResource,name=*"),null);
		for (ObjectName on : ons) {
			String name = on.getKeyProperty("name");
			b = new InfoBean(name);
			al = context.getAsMap(mbs.getAttributes(on, new String[] { PID, STATE, LAST_SUCCESSFUL_START, DETACH_TIME, LAST_ERROR}));
			b.put(PID, context.getValue(al, PID));
			b.put(STATE,toStateString(context.getValue(al, STATE)));
			b.put(LAST_SUCCESSFUL_START,context.getValue(al, LAST_SUCCESSFUL_START));
			b.put(DETACH_TIME,context.getValue(al, DETACH_TIME));
			b.put(LAST_ERROR,context.getValue(al, LAST_ERROR));

			b.add(new InfoBeanAction("stop", "stop").setParam("name", name).setParam("action", "stop"));
			b.add(new InfoBeanAction("restart", "restart").setParam("name", name).setParam("action", "restart"));
			b.add(new InfoBeanAction("detach", "detach").setParam("name", name).setParam("action", "detach"));
			
			beans.add(b);
		}
		Collections.sort(beans, new Comparator<InfoBean>() {
			public int compare(InfoBean o1, InfoBean o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
	}

	private String toStateString(String value) {
		int state = Integer.parseInt(value);
		switch (state) {
			case IWorkerProcess.DETACHED: return "detached";
			case IWorkerProcess.NOT_STARTED: return "not started";
			case IWorkerProcess.STARTED: return "started";
			case IWorkerProcess.STARTING: return "starting";
			case IWorkerProcess.STOPPING: return "stopping";
		}
		return "unknown";
	}

}
