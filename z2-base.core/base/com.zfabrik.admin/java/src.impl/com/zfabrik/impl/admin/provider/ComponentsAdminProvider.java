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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.impl.admin.web.vm.InfoBeanAction;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.util.expression.X;

public class ComponentsAdminProvider implements IAdminProvider {
	IAdminProviderContext context;

	@Override
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
		// TODO Auto-generated method stub

	}

	@SuppressWarnings("unchecked")
	@Override
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		// components
		Set<String> all = new HashSet<String>(IComponentsManager.INSTANCE.findComponents(X.var(IComponentDescriptor.COMPONENT_TYPE).neq(
				X.val(""))));
		ObjectName on = ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.resources.NamespaceImpl,name=com.zfabrik.components");
		Set<String> loaded = new HashSet<String>((Collection<String>) mbs.getAttribute(on, "AllResourceNames"));
		for (String c : all) {
			IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(c);
			b = new InfoBean(c);
			b.put("managed", Boolean.toString(loaded.contains(c)));

			StringBuilder sb = new StringBuilder(50);
			String ri = d.getProperty(IComponentDescriptor.REVISION_INFO);
			if (ri!=null && (ri=ri.trim()).length()>0) {
				sb.append(ri).append(" (").append(Long.toString(d.getRevision())).append(")");
			} else {
				sb.append(Long.toString(d.getRevision()));
			}			
			b.put("revision", sb.toString());
			b.add(new InfoBeanAction("invalidateComponent", "invalidate").setParam("componentName", c));
			b.add(new InfoBeanAction("invalidateComponentAndVerify", "inv&Verify").setParam("componentName", c));
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
		context.setTabular(true);
		this.context = context;
	}

}
