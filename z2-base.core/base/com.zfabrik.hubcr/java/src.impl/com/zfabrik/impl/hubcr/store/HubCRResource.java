/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.hubcr.store;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.impl.hubcr.store.HubCRManager.Reporter;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.runtime.Foundation;

/**
 * Life cycle of HubCR provider implementation. Also does JMX registration.
 * <p>
 * This resource may be loaded on home and worker. But the actual manager
 * and the JMX beans will only be loaded on worker nodes (and otherwise throw an exception).
 * 
 * <p>
 * The following configurations are supported:
 * <ul>
 * <li><b>hubcr.eviction.delay</b> - specifies the max keep time of older component revisions than the most current in hours. Defaults to 168h</li>
 * <li><b>hubcr.scanOnSync</b> - if set to <code>true</code>, every sync completion will trigger an update of the HubCR</li>
 * <li><b>hubcr.ignore.types</b> - Comma-separated list of component types to not put in the repository</li>
 * <li><b>hubcr.ignore.prefixes</b> - Comma-separated list of component name prefixes to filter out components before putting them in the repository</li>
 * </ul>
 * 
 * By default the component types <code>com.zfabrik.svncr</code>, <code>com.zfabrik.gitcr</code>, <code>com.zfabrik.hubcr</code> as well as the prefix <code>com.zfabrik.hubcr/</code> are in effect.
 * Also, no components of the local repo will be served. 
 * 
 * Note, this is the serving side. See also <a href="/javadoc/com.zfabrik.boot.hubcr!2Fjava/impl/index.html">ComponentRepositoryImpl</a> on how
 * to set up a consuming component repository.
 * <p>
 * To enable provisioning via the HubCR visit the manager configuration at <code>com.zfabrik.hubcr/manager</code> and enable the
 * Web application <code>com.zfabrik.hubcr/web</code> by configuring a suitable state dependency. Please do not start the 
 * web app by a dependency declared outside of the served scope (which includes the module <code>com.zfabrik.hubcr</code>) as that
 * will lead to unresolvable dependencies on the consuming side.
 * 
 * @author hb
 *
 */
public class HubCRResource extends Resource {
	
	private static final String NAME = "com.zfabrik.hubcr/manager";
	/**
	 * Eviction of components in repo buffer in hours
	 */
	private final static String HUBCR_EVICTION = "hubcr.eviction.delay";
	/**
	 * The HubCR can be configured to update on every sync.
	 */
	private final static String HUBCR_SCANONSYNC = "hubcr.scanOnSync";
	/**
	 * The HubCR can be configured to never serve some types by a comma-separated list of component types
	 */
	private final static String HUBCR_IGNORE_TYPES = "hubcr.ignore.types";
	/**
	 * The HubCR can be configured to never serve some components by a comma-separated list of prefixes
	 */
	private final static String HUBCR_IGNORE_PREFIXES = "hubcr.ignore.prefixes";

	private HubCRManager repo;
	
	// config
	private boolean configured;
	private long evictionDelay;
	private boolean scanOnSync;
	private Set<String> ignoreTypes, ignorePrefices;
	
	// begin JMX
	private ObjectName on;
	
	public interface ManagerMBean {
		Date getRevision();
		String scan();
	}

	private class  Manager implements ManagerMBean {
		public synchronized String scan() {
			final StringBuilder sb = new StringBuilder();
			if (_isStarted()) {
				repo.scan(new Reporter() {
					public void log(Level level, String message) {
						sb.append(message).append("\n");
					}
				});
			} else {
				throw new IllegalStateException("HubCR not initialized");
			}
			return sb.toString();
		}
		
		@Override
		public synchronized Date getRevision()  {
			if (_isStarted() && !repo.isInitial()) {
				try {
					return new Date(repo.getDB().getRevision());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new IllegalStateException("HubCR is still initial");
			}
			
		}
	};
	
	private ManagerMBean mbean = new Manager();
	// end JMX
	
	
	// Resouce.as
	@Override
	public synchronized <T> T as(Class<T> clz) {
		if (HubCRResource.class.equals(clz)) {
			return clz.cast(this);
		}
		if (HubCRManager.class.equals(clz)) {
			_load();
			return clz.cast(this.repo);
		}
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(new IDependencyComponent() {
				public synchronized void prepare() {
					_load();
				}
			});
		}
		return super.as(clz);
	}
	
	// load just config (home and worker)
	private void _loadConfig() {
		if (!this.configured) {
			IComponentDescriptor d = handle().as(IComponentDescriptor.class);
			String eds = d.getProperties().getProperty(HUBCR_EVICTION, "").trim();
			this.evictionDelay = 7*24*60*60*1000;
			if (eds.length()>0) {
				this.evictionDelay = Long.parseLong(eds)*60*60*1000;
			}
			this.scanOnSync = Boolean.parseBoolean(d.getProperty(HUBCR_SCANONSYNC));
			
			
			this.ignorePrefices = new HashSet<String>();
			_readStringList(this.ignorePrefices,d.getProperty(HUBCR_IGNORE_PREFIXES));
			this.ignoreTypes = new HashSet<String>();
			_readStringList(this.ignoreTypes,d.getProperty(HUBCR_IGNORE_TYPES));
			
			this.configured = true;
		}
	}
	
	private void _readStringList(Set<String> set, String list) {
		if (list!=null) {
			StringTokenizer st = new StringTokenizer(list,",");
			while (st.hasMoreTokens()) {
				String v = st.nextToken().trim();
				if (v.length()>0) {
					set.add(v);
				}
			}
		}
	}

	// really load and initialize (worker only)
	private void _load() {
		this._loadConfig();
		if (!_isStarted()) {
			if (!Foundation.isWorker()) {
				throw new IllegalStateException("HubCR manager is not to be run on home process");
			}
			
			try {
				logger.info(String.format(
					"Starting HubCR with old-versions-eviction delay %dms (%.2f days or %.2f hours). ScanOnSync is %s.",
					this.evictionDelay,
					(float) this.evictionDelay/(24*60*60*1000),
					(float) this.evictionDelay/(60*60*1000),
					this.scanOnSync? "enabled":"disabled"
				));

				this.repo = new HubCRManager(this.evictionDelay,this.ignoreTypes,this.ignorePrefices);
				handle().adjust(0,Long.MAX_VALUE,IResourceHandle.HARD);
				
				if (this.repo.isInitial()) {
					logger.info("Running initial fill...");
					_scan();
				} else {
					logger.info("Current HubCR revision is of "+new Date(this.repo.getDB().getRevision()));
				}
			} catch (Exception e) {
				logger.log(Level.SEVERE,"Error during HubCR initialization",e);
			}
	
			try {
				this.on = ObjectName.getInstance("zfabrik:type="+HubCRResource.class.getName());
				ManagementFactory.getPlatformMBeanServer().registerMBean(this.mbean,this.on);
			} catch (Exception e) {
				throw new IllegalStateException("HubCR Mbean registration failed", e);
			}

		}
	}

	// scan and log 
	private void _scan() {
		this.repo.scan(new Reporter() {
			public void log(Level level, String message) {
				logger.log(level,message);
			}
		});
	}
	
	// synchronization 
	public synchronized void scanOnSync() {
		if (isScanOnSync() && Foundation.isWorker() && _isStarted()) {
			this._scan();
		}
	}
	
	public boolean _isStarted() {
		return this.repo!=null;
	}
	
	// deciding whether to scan upon sync (only config)
	public synchronized boolean isScanOnSync() {
		this._loadConfig();
		return this.scanOnSync;		
	}
	// end synchronizer

	
	// Resouce.invalidate
	@Override
	public synchronized void invalidate() throws ResourceBusyException {
		try {
			if (this.on!=null) {
				try {
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
				} catch (Exception e) {
					logger.log(Level.WARNING,"Error unregistering MBean "+this.on,e);
				}
			}
		} finally {
			this.configured = false;
			this.repo = null;
			this.on = null;
		}
	}

	// utility accessor
	public static HubCRManager getManager() {
		return IComponentsLookup.INSTANCE.lookup(NAME, HubCRManager.class);
	}

	// utility accessor
	public static HubCRResource getResource() {
		return IComponentsLookup.INSTANCE.lookup(NAME, HubCRResource.class);
	}

	private final static Logger logger = Logger.getLogger(HubCRResource.class.getName());
}
