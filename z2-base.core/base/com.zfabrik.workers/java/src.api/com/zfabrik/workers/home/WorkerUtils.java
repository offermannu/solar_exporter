/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.home;

import com.zfabrik.components.IComponentsLookup;

/**
 * Some utility methods around worker process handling
 * @author hb
 *
 */
public class WorkerUtils {

	public static String getWorkerComponentName(String workerName) {
		if (workerName!=null) {
			int p = workerName.lastIndexOf('@');
			if (p<0) {
				return workerName;
			}
			return workerName.substring(0,p);
		}
		return null;
	}

	/**
	 * Retrieve a worker process instance (to be called from home only).
	 * 
	 * @param id worker process component of fully qualified worker name
	 * @return worker process handle or <code>null</code> if not found 
	 */
	public static IWorkerProcess getWorkerProcess(String workerName) {
		String cn = getWorkerComponentName(workerName);
		IWorkerHorde wh = IComponentsLookup.INSTANCE.lookup(cn,IWorkerHorde.class);
		if (wh!=null) {
			if (cn.equals(workerName)) {
				return wh.getActiveWorkerProcess();
			} else {
				return wh.getWorkerProcesses().get(workerName);
			}
		}
		return null;
	}
	
}
