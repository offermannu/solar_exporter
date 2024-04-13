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

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.provider.util.FSCRDBComponent;
import com.zfabrik.components.provider.util.FSComponentRepositoryHelper;
import com.zfabrik.components.provider.util.Lock;
import com.zfabrik.dev.util.ComponentCopyTool;
import com.zfabrik.hubcr.RemoteComponentRepositoryDB;
import com.zfabrik.util.expression.X;
import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.runtime.Foundation;

/**
 * Manages a providing repository that is filled as at times
 * by an administrative action (scan). This implementation
 * heavily relies on the standard persistent repository
 * layout.
 *
 * @author hb
 */
public class HubCRManager {
	/**
	 * Component types we ignore in provisioning
	 */
	@SuppressWarnings("serial")
	private static final Set<String> IGNORE_TYPES = new HashSet<String>() {{
		add("com.zfabrik.svncr");
		add("com.zfabrik.gitcr");
		add("com.zfabrik.hubcr");
		add("com.zfabrik.mvncr");
	}};

	/**
	 * Component name prefixces we ignore in provisioning
	 */
	@SuppressWarnings("serial")
	private static final Set<String> IGNORE_COMPONENT_PREFICES = new HashSet<String>() {{
		add("com.zfabrik.hubcr/");
	}};

	private Set<String> ignoreTypes, ignorePrefices;

    private FSComponentRepositoryHelper<RemoteComponentRepositoryDB> repoHelper;
	private File storeRoot;
	private RemoteComponentRepositoryDB db;

	public HubCRManager(long eviction, Set<String> ignoreTypes, Set<String> ignorePrefices) {
		try {
			this.storeRoot =
				new File(
					Foundation.getProperties().getProperty(Foundation.HOME_LAYOUT_DATA),
					"hubcr"
				);
			this.storeRoot.mkdirs();
			this.repoHelper = new FSComponentRepositoryHelper<RemoteComponentRepositoryDB>(RemoteComponentRepositoryDB.class, this.storeRoot);
			this.repoHelper.setEvictionDelay(eviction);

			this.ignorePrefices = new HashSet<String>(IGNORE_COMPONENT_PREFICES);
			if (ignorePrefices!=null) {
				this.ignorePrefices.addAll(ignorePrefices);
			}

			this.ignoreTypes = new HashSet<String>(IGNORE_TYPES);
			if (ignoreTypes!=null) {
				this.ignoreTypes.addAll(ignoreTypes);
			}

			logger.info(
				String.format(
					"Filtering types %s. Filtering prefixes %s",
					this.ignoreTypes.toString(),
					this.ignorePrefices.toString()
				)
			);
			// read the latest DB (if there is one);
			checkDB(false);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize remote repo manager",e);
		}
	}

	private void checkDB(boolean fail) {
		if (this.db == null) {
			try {
				Lock lock = this.repoHelper.lockDB();
				try {
					this.db = this.repoHelper.readDB();
				} finally {
					lock.close();
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to load HubCR DB", e);
			}
		}
		if (this.db==null && fail) {
			throw new IllegalStateException("HubCR is still initial");
		}
	}

	/**
	 * check whether the repo is initial
	 */
	public synchronized boolean isInitial() {
		return this.db == null;
	}

	/**
	 * Helper interface to have feedback provided from {@link #scan(Reporter)} calls.
	 */
	public interface Reporter {
		void log(Level level, String message);
	}


    /**
     * Scan once more. Update the persistent. Updates the db
     */
	public synchronized void scan(Reporter reporter) {
		try {
			Lock lock = this.repoHelper.lockDB();
			try {
				RemoteComponentRepositoryDB oldDb = this.repoHelper.readDB();
				if (oldDb==null) {
					oldDb = new RemoteComponentRepositoryDB();
				} else {
					// clean up
					for (String cn:oldDb.getComponents().keySet()) {
						this.repoHelper.evictLocalComponent(cn);
					}
				}
				RemoteComponentRepositoryDB newDb = new RemoteComponentRepositoryDB();
				newDb.setRevision(System.currentTimeMillis());
				// check for what we have, what is there, and move stuff when missing

				Collection<String> all = IComponentsManager.INSTANCE.findComponents(X.val(true));

				for (String componentName : all) {
					IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(componentName);
					if (d!=null) {
						// some components are not supposed to be even visible. Filter those.
						if (isPermissable(d)) {

							FSCRDBComponent c = oldDb.getComponents().get(componentName);

							if (c==null || c.getRevision()!=d.getRevision()) {
								// we don't have it yet or the revisions do not match.
								// inspect it
								FSCRDBComponent n = ComponentCopyTool.inspectComponent(componentName);
								if (n.isHasFolder()) {
									// copy resources
									File folder = this.repoHelper.getComponentFolder(n);
									if (folder.exists()) {
										FileUtils.delete(folder);
									}
									folder.mkdirs();
									ComponentCopyTool.copyComponentResources(componentName, folder, ComponentCopyTool.Mode.NOJAVA);
								}
								if (c==null) {
									reporter.log(Level.INFO,"Added "+componentName+"@"+n.getRevision());
								} else {
									reporter.log(Level.INFO,"Updated "+componentName+"@"+n.getRevision());
								}
								// add to Db
								newDb.putComponent(componentName, n);
							} else {
								// keep it
								reporter.log(Level.FINE,"Kept "+componentName+"@"+c.getRevision());
								newDb.putComponent(componentName, new FSCRDBComponent(c));
							}
						} else {
							if (oldDb.getComponents().containsKey(componentName)) {
								reporter.log(Level.INFO,"Removed "+componentName);
							}
						}
					}
				}

				if (newDb.equals(oldDb)) {
					reporter.log(Level.INFO,"Scan found no updates");
				}

				// persist new db
				this.repoHelper.saveDB(newDb);
				// make it the currently known one
				this.db = newDb;
				logger.info("Storing DB rev "+this.db.getRevision());
			} finally {
				lock.close();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to update remote cr database",e);
		}
	}

	public synchronized RemoteComponentRepositoryDB getDB() throws Exception {
		checkDB(true);
		logger.info("Providing DB rev "+this.db.getRevision());
		return new RemoteComponentRepositoryDB(this.db);
	}

	public synchronized File getComponentFolder(String componentName, long revision) {
		checkDB(true);
		FSCRDBComponent c = new FSCRDBComponent(componentName);
		c.setHasFile(false);
		c.setHasFolder(true);
		c.setRevision(revision);
		return this.repoHelper.getComponentFolder(c);
	}

	private boolean isPermissable(IComponentDescriptor d) {
		if (d.getProperty(IComponentsManager.COMPONENT_REPO_IMPLEMENTATION)==null) {
			// must be local repo. We won't export
			return false;
		}
		if (this.ignoreTypes.contains(d.getType())) {
			return false;
		}

		for (String p : this.ignorePrefices) {
			if (d.getName().startsWith(p)) {
				return false;
			}
		}

		return true;
	}


	private final static Logger logger = Logger.getLogger(HubCRManager.class.getName());
}