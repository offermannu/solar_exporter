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

public class NamespacesAdminProvider implements IAdminProvider {
	private static final String RMNS_NUM_SUCCESS_FULL_QUERIES = "NumSuccessFullQueries";
	private static final String RMNS_NUM_SHRINKS = "NumShrinks";
	private static final String RMNS_NUM_QUERIES = "NumQueries";
	private static final String RMNS_NUM_PROVIDER_QUERIES = "NumProviderQueries";
	private static final String RMNS_NUM_CLEAN_UPS = "NumCleanUps";
	private static final String RMNS_MAX_SIZE = "MaxSize";
	private static final String RMNS_LAST_RESET_TIME = "LastResetTime";
	private static final String RMNS_CURRENT_SIZE = "CurrentSize";
	
	IAdminProviderContext context;

	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
	}

	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		Map<String, Object> al;
		Set<ObjectName> ons;
		// RM namespaces
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.resources.NamespaceImpl,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean(on.getKeyProperty("name"));
			al = context.getAsMap(mbs.getAttributes(on, new String[] { RMNS_CURRENT_SIZE, RMNS_LAST_RESET_TIME, RMNS_MAX_SIZE, RMNS_NUM_CLEAN_UPS,
					RMNS_NUM_PROVIDER_QUERIES, RMNS_NUM_QUERIES, RMNS_NUM_SHRINKS, RMNS_NUM_SUCCESS_FULL_QUERIES }));
		
			for (String a : al.keySet()) {
				b.put(a, context.getValue(al, a));
			}
			b.sortAttributesByKey();
			beans.add(b);
		}
		Collections.sort(beans, new Comparator<InfoBean>() {
			public int compare(InfoBean o1, InfoBean o2) {
				return o1.getTitle().compareTo(o2.getTitle());
			}
		});
	}

	public void init(IAdminProviderContext context) {
		context.setTabular(false);
		this.context = context;
	}

}
