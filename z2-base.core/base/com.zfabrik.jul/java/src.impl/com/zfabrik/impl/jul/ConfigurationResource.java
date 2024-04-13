/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.jul;

import java.io.File;
import java.io.FileInputStream;
import java.util.logging.LogManager;

import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

/**
 * java util logging configuration resource. Can be triggered by system state dependency or
 * via lookup as runnable.
 * @author hb
 *
 */

public class ConfigurationResource extends Resource implements IDependencyComponent, Runnable {
	private boolean loaded;
	private String name;
	
	public ConfigurationResource(String name) {
		this.name = name;
	}

	public synchronized <T> T as(Class<T> clz) {
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		if (Runnable.class.equals(clz)) {
			return clz.cast(this);
		}
		return super.as(clz);
	}

	public void prepare() {
		apply();
	}

	public void run() {
		apply();
	}
	
	public synchronized void apply() {
		if (!this.loaded) {
			try {
				// simply look for a logging.properties file.
				File f = IComponentsManager.INSTANCE.retrieve(this.name);
				if (!f.exists() || !f.isDirectory()) {
					throw new IllegalStateException("Folder "+f+" not found: "+this.name);
				}
				f = new File(f,"logging.properties");
				if (!f.exists() || f.isDirectory()) {
					throw new IllegalStateException("Configuration file "+f+" not found: "+this.name);
				}
				FileInputStream fin = new FileInputStream(f);
				try {
					LogManager.getLogManager().readConfiguration(fin);
				} finally {
					fin.close();
				}
			} catch (Exception e) {
				throw new RuntimeException("Failed to load JUL logging configuration: "+this.name,e);
			}
			this.loaded = true;
		}
	}

	public synchronized void invalidate() throws ResourceBusyException {
		this.loaded = false;
	}
}
