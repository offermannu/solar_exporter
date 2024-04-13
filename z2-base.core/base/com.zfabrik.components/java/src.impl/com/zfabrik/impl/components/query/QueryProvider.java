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

import com.zfabrik.resources.provider.IResourceProvider;
import com.zfabrik.resources.provider.IResourceProviderContext;
import com.zfabrik.resources.provider.Resource;

public class QueryProvider implements IResourceProvider {
//	private IResourceProviderContext c;

	public Resource get(String name) {
		return new QueryResource(name);
	}

	public void init(IResourceProviderContext c) {
//		this.c=c;
	}

}
