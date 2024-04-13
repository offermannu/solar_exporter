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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.impl.admin.provider.ComponentsAdminProvider;
import com.zfabrik.impl.admin.provider.IAdminProvider;
import com.zfabrik.impl.admin.provider.IAdminProviderContext;
import com.zfabrik.impl.admin.provider.LoggerAdminProvider;
import com.zfabrik.impl.admin.provider.NamespacesAdminProvider;
import com.zfabrik.impl.admin.provider.OverviewAdminProvider;
import com.zfabrik.impl.admin.provider.SystemPropertiesAdminProvider;
import com.zfabrik.impl.admin.provider.TimersAdminProvider;
import com.zfabrik.impl.admin.provider.WebAdminProvider;
import com.zfabrik.impl.admin.provider.WorkerAdminProvider;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.util.StringUtils;
import com.zfabrik.util.microweb.actions.OutCome;
import com.zfabrik.workers.home.IHomeLayout;
import com.zfabrik.workers.worker.HomeHandle;

public class Admin implements IAdminProviderContext {
	private static final String HOME = "<home>";

	private HttpServletRequest request;
	private boolean tabular;
	private String process;
	private String group = "Overview";
	private String fileName;
	private String resourceName;
	private String componentName;

	private final static DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
	private List<String> messages = new LinkedList<String>();

	private Map<String,IAdminProvider> providers = new TreeMap<String, IAdminProvider>();
	
	public Admin() {
		// setup
		providers.put("Overview", new OverviewAdminProvider());
		providers.put("Web", new WebAdminProvider());
		providers.put("Timers", new TimersAdminProvider());
		providers.put("System Properties", new SystemPropertiesAdminProvider());
		providers.put("Namespaces", new NamespacesAdminProvider());
		providers.put("Components", new ComponentsAdminProvider());
		providers.put("Logger", new LoggerAdminProvider());
		providers.put("Workers", new WorkerAdminProvider());
	}
	
	
	public OutCome init(HttpServletRequest request) {
		this.request = request;
		return null;
	}
	
	@SuppressWarnings({"rawtypes" })
	public OutCome doByDefault() throws Exception {
		IAdminProvider p = _provider();
		p.init(this);
		
		Map<String,String> params = new HashMap<String, String>();
		for (Object  o : this.request.getParameterMap().entrySet()) {
			Map.Entry e = (Map.Entry) o;
			params.put((String)e.getKey(),null);
			if (e.getValue()!=null) {
				String[] vs = (String[]) e.getValue();
				if (vs.length>0) {
					params.put((String)e.getKey(),vs[0]);
				}
			}
		}
		p.action(_getMbs(), params, messages);
		return OutCome.forward("/WEB-INF/admin.jsp");
	}

	@Override
	public String getValue(Map<String, Object> map, String key) {
		Object r = map.get(key);
		if (r == null) {
			return "(n.a.)";
		}
		if (r instanceof Date) {
			return df.format((Date) r);
		}
		return r.toString();
	}

	@Override
	public Number getNumber(Map<String, Object> map, String key) {
		Object r = map.get(key);
		if (r instanceof Number) {
			return (Number)r;
		}
		return -1;
	}

	
	public Map<String, Object> getAsMap(AttributeList al) {
		Map<String, Object> res = new HashMap<String, Object>(al.size());
		for (Attribute a : al.asList()) {
			res.put(a.getName(), a.getValue());
		}
		return res;
	}

	public String format(Date date) {
		return df.format(date);
	}	
	
	// ------------- params ----------------

	public void setProcess(String process) {
		if (HOME.equals(process)) {
			process = null;
		}
		this.process = process;
	}

	public String getProcess() {
		return (this.process==null? HOME : this.process);
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public void setFileName(String fileName) {
		this.fileName = StringUtils.trimAndNormalizeToNull(fileName);
	}

	public String getFileName() {
		return fileName;
	}

	public void setComponentName(String componentName) {
		this.componentName = StringUtils.trimAndNormalizeToNull(componentName);
	}

	public String getComponentName() {
		return this.componentName;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = StringUtils.trimAndNormalizeToNull(resourceName);
	}

	public String getResourceName() {
		return resourceName;
	}

	public String getTitle() {
		return "Virtual Machine (" + this._processName() + ") overview";
	}

	public void setTabular(boolean tabular) {
		this.tabular = tabular;
	}

	public boolean isTabular() {
		return tabular;
	}

	// key -> display name
	private Map<String,String> processes;

	public Map<String,String> getProcesses() throws Exception {
		if (this.processes == null) {
			IHomeMBeanServer mbs = _getMbs(null);
			Set<ObjectName> names = mbs
					.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.workers.home.WorkerProcessResource,*"), null);
			Map<String,String> processes = new TreeMap<String,String>();
			processes.put(HOME,HOME);
			for (ObjectName on : names) {
				String pid;
				try {
					pid =  (String) mbs.getAttribute(on, "PID");
				} catch (Exception e) {
					pid = null;
				}
				String name = (String) mbs.getAttribute(on, "Name");
				processes.put(name, name+ (pid!=null? " ("+pid+")":""));
			}
			this.processes = processes;
		}
		return this.processes;
	}

	public List<String> getGroups() {
		return new ArrayList<String>(this.providers.keySet());
	}

	public List<String> getMessages() {
		return messages;
	}

	/*
	 * The actual data provisioning
	 */
	public class Data {
		private List<InfoBean> beans;

		public List<InfoBean> getBeans() throws Exception {
			if (this.beans==null) {
				IAdminProvider p = _provider();
				IHomeMBeanServer mbs = _getMbs();
				List<InfoBean> beans = new LinkedList<InfoBean>();
				p.addBeans(mbs, beans);
				this.beans = beans;
			}
			return this.beans;
		}

		private InfoTable table;

		public InfoTable getTable() throws Exception {
			if (table == null) {
				List<InfoBean> beans = getBeans();
				List<String> cols = new LinkedList<String>();
				cols.add("title");
				for (InfoBean b : beans) {
					for (InfoBeanAttribute a : b.getAttributes()) {
						if (!cols.contains(a.getName())) {
							cols.add(a.getName());
						}
					}
				}
				InfoTable t = new InfoTable(cols);
				for (InfoBean b : beans) {
					Map<String, String> r = new HashMap<String, String>();
					r.put("title", b.getTitle());
					for (InfoBeanAttribute a : b.getAttributes()) {
						r.put(a.getName(), a.getValue());
					}
					t.addRow(new InfoTableRow(r, b.getActions()));
				}
				this.table = t;
			}
			return this.table;
		}
	}

	private Data data = new Data();

	public Data getData() {
		return data;
	};

	// ------------- actions ----------------

	private void _addMessage(String m) {
		this.messages.add(m);
	}

	// invoke GC
	public OutCome doCallGC() throws Exception {
		IHomeMBeanServer mbs = _getMbs();
		mbs.invoke(ObjectName.getInstance("java.lang:type=Memory"), "gc", null, null);
		_addMessage("Triggered GC on " + _processName());
		return doByDefault();
	}

	// do sync
	public OutCome doSynchronize() throws Exception {
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
		mbs.invoke(on, "synchronize", null, null);
		_addMessage("Triggered synchronization of <home>");
		return doByDefault();
	}

	// do verify
	public OutCome doVerify() throws Exception {
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
		mbs.invoke(on, "verify", null, null);
		_addMessage("Triggered verification of <home>");
		return doByDefault();
	}
	
	// write a heap dump
	public OutCome doWriteHeapDump() throws Exception {
		IHomeMBeanServer mbs = _getMbs();
		if (this.fileName == null || this.fileName.length() == 0) {
			this.fileName = "heapdump.hprof";
		}
		if (this.fileName.lastIndexOf('.') < 0) {
			this.fileName += ".hprof";
		}
		mbs.invoke(ObjectName.getInstance("com.sun.management:type=HotSpotDiagnostic"), "dumpHeap", new Object[] { this.fileName, true },
				new String[] { String.class.getName(), boolean.class.getName() });
		_addMessage("Wrote heap dump of " + _processName() + " to file " + this.fileName);
		return doByDefault();
	}

	// do invalidate a resource (globally)
	public OutCome doInvalidateResource() throws Exception {
		if (this.resourceName != null) {
			if (this.resourceName.startsWith(IComponentsLookup.COMPONENTS + "/")) {
				this.componentName = this.resourceName.substring(IComponentsLookup.COMPONENTS.length() + 1);
			}
			Collection<String> invs = Arrays.asList(new String[] { this.resourceName });
			HomeHandle.instance().broadcastInvalidations(invs, IHomeLayout.SCOPE_HOME_LAYOUT & (~IHomeLayout.SCOPE_HOME) ); 
			_addMessage("Triggered invalidation of " + invs);
		}
		return doByDefault();
	}

	// do invalidate a component
	public OutCome doInvalidateComponent() throws Exception {
		String cn = getComponentName();
		if (cn != null) {
			this.resourceName = IComponentsLookup.COMPONENTS + "/" + cn;
			return doInvalidateResource();
		}
		return doByDefault();
	}
				     
	public OutCome doInvalidateComponentAndVerify() throws Exception {
		String cn = getComponentName();
		if (cn != null) {
			this.resourceName = IComponentsLookup.COMPONENTS + "/" + cn;
			doInvalidateResource();
			return doVerify();
		}
		return doByDefault();
	}
	

	
	// ----------------

	private String _processName() {
		return this.process == null ? HOME : this.process;
	}

	private IAdminProvider _provider() {
		IAdminProvider p = providers.get(group);
		if (p==null) {
			p = providers.get("Overview");
		}
		return p;
	}

	private IHomeMBeanServer _getMbs() {
		return _getMbs(this.process);
	}

	private IHomeMBeanServer _getMbs(String pr) {
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(pr);
		return mbs;
	}

}
