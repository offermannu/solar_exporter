/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.query;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.components.query.IComponentQueriesLookup;
import com.zfabrik.components.query.IComponentQuery;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.IResourceLookup;

public class QueryVerification {

	public static void execute() {
		// iterate over all component queries and verify and invalidate if
		// failing.
		IResourceLookup rl = IComponentQueriesLookup.INSTANCE;
		List<String> all = rl.list();
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Verifying " + all.size() + " component queries");
		}
		int i = 0;
		for (String qn : all) {
			IResourceHandle rh = rl.query(qn, IResourceHandle.class);
			if (rh != null) {
				if (!rh.as(IComponentQuery.class).verify()) {
					if (logger.isLoggable(Level.FINER)) {
						logger.finer("Invalidating component query \"" + qn + "\" due to failed verification");
					}
					i++;
					rh.invalidate(true);
				}
			}
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Invalidated  " + i + " component queries");
		}
	}

	private final static Logger logger = Logger.getLogger(QueryVerification.class.getName());
}
