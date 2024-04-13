/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.devcr;

import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

public class DevRepoResource extends Resource implements IDependencyComponent {
	private DevRepoImpl d;

	public <T> T as(Class<T> clz) {
		if (DevRepoImpl.has(clz)) {
			_load();
			return d.as(clz);
		}
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}

	@Override
	public void prepare() {
		_load();
	}
	
	public synchronized void _load() {
		if (d==null) {
			d = new DevRepoImpl(handle().getResourceInfo().getName());
		}
	}

	@Override
	public synchronized void invalidate()
			throws ResourceBusyException {
		if (d!=null) {
			try {
				d.stop();
			} finally {
				d=null;
			}
		}
	}

}
