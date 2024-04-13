/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.servletjsp.security;

import javax.security.auth.spi.LoginModule;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

/**
 * Provides a login module from a component 
 * @author hb
 */
public class LoginModuleResource extends Resource {
	private String name;
	private Class<?> clazz;
	
	public LoginModuleResource(String name) {
		this.name = name;
	}
	
	public synchronized <T> T as(Class<T> clz) {
		if (LoginModule.class.equals(clz)) {
			if (this.clazz==null) {
				this.clazz = JavaComponentUtil.loadImplementationClassFromJavaComponent(
					this.name,
					IComponentDescriptor.COMPONENT_CLZ,
					handle()
				);
			}
			try {
				return clz.cast(this.clazz.getConstructor().newInstance());
			} catch (Exception e) {
				throw new IllegalStateException("Failed to instantiate login module: "+name,e);
			}
		}
		return super.as(clz);
	}
	
	public synchronized void invalidate() throws ResourceBusyException {
		this.clazz = null;
	}
	
}
