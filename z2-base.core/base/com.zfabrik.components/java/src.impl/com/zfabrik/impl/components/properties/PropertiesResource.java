/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.properties;

import java.util.Properties;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

public class PropertiesResource extends Resource {
	private Properties props;
	
	public PropertiesResource(String name) {}
	
	@Override
	public synchronized <T> T as(Class<T> clz) {
		if (Properties.class.equals(clz)) {
			_load();
			return clz.cast(this.props);
		}
		return super.as(clz);
	}

	private void _load() {
		if (this.props==null) {
			this.props = handle().as(IComponentDescriptor.class).getProperties();
		}
	}

	@Override
	public synchronized void invalidate() throws ResourceBusyException {
		this.props = null;
	}

}
