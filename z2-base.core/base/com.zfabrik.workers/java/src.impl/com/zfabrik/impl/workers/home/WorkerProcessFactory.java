/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;


import com.zfabrik.components.provider.IComponentFactory;
import com.zfabrik.resources.provider.Resource;

public class WorkerProcessFactory implements IComponentFactory {

	public Resource createComponentResource(String name) {
		return new WorkerProcessResource(name);
	}

}
