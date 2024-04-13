/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.local;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.util.sync.ISynchronization;

/**
 * public interface of dev repo.
 * @author hb
 *
 */

public abstract class DevRepository {
	protected volatile static DevRepository instance;
	
	/**
	 * Property to be specified in the component descriptor to identify the 
	 * associated project for the case of a component level check out.
	 */
	public final static String PROP_PROJECT = "com.zfabrik.dev.project";
	
	public static DevRepository instance() {
		return instance;
	}

	/**
	 * System property holding a comma separated list of path names (relative to the current working folder of the z2 process)
	 */
	public static final String LOCAL_WORKSPACE = "com.zfabrik.dev.local.workspace";

	/**
	 * Search depths for armed modules (those that have a LOCAL file). Level 1 is the workspace folder, Level 2  adds folders in the
	 * workspace folders and so on. Defaults to 3. Can be set on the dev repo component or as system property overriding the former.
	 */
	public static final String LOCAL_DEPTH = "com.zfabrik.dev.local.depth";
	
	/**
	 * retrieve id of a current update, or zero if there was none
	 * @return
	 */
	public abstract long getUpdated();
	/**
	 * should be same update key as retrieved from {@link #getUpdated()}
	 * @param up
	 */
	public abstract void resetUpdated(long up);

	/**
	 * drop current db (and reload on next demand) if older than rev
	 */
	public abstract void releaseDB(long rev);
	
	/**
	 * update db and derive invalidations
	 * @param sync
	 */
	public abstract void preInvalidation(ISynchronization sync);

	/**
	 * Retrieve all currently set workspace folders
	 */
	public static Collection<Path> getWorkspaces() {
		Set<Path> result = new HashSet<>();
		String wss = Foundation.getProperties().getProperty(DevRepository.LOCAL_WORKSPACE);
		if (wss!=null) {
			// workspace setting is a list of dirs separated by ','
			for (String ws : wss.split(",")) {
				ws = ws.trim();
				if (ws.length()>0) {
					result.add(Paths.get(ws));
				}
			}
		}
		return result;
	}
	
}
