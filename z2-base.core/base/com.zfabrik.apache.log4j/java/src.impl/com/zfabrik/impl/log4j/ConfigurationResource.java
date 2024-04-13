/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.log4j;

import java.io.File;
import java.util.logging.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

/**
 * log 4j configuration resource. Can be triggered by system state dependency or
 * via lookup as runnable.
 * @author hb
 *
 */
public class ConfigurationResource extends Resource implements IDependencyComponent, Runnable {
	private final static Logger LOG = Logger.getLogger(ConfigurationResource.class.getName()); 
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
				// simply look for a log4j.properties file.
				File f = IComponentsManager.INSTANCE.retrieve(this.name);
				if (!f.exists() || !f.isDirectory()) {
					throw new IllegalStateException("Folder "+f+" not found: "+this.name);
				}
				f = new File(f,"log4j.properties");
				if (!f.exists() || f.isDirectory()) {
					throw new IllegalStateException("Configuration file "+f+" not found: "+this.name);
				}
				// initialize (unless that has happened already)
				LoggerContext ctx = Configurator.initialize(
				    this.name, 
				    null, 
				    f.getAbsolutePath()
				);
				// reconfigure, if it was already there (in which case Log4j will simply do nothing in initialize)
				ctx.setConfigLocation(f.getAbsoluteFile().toURI());
				LOG.info("Loaded Log4j2 config from "+this.name);
			} catch (Exception e) {
				throw new RuntimeException("Failed to load log4j configuration: "+this.name,e);
			}
			this.loaded = true;
		}
	}

	public synchronized void invalidate() throws ResourceBusyException {
		this.loaded = false;
	}
}
