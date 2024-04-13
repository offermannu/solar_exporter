/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.hubcr.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.zfabrik.impl.hubcr.store.HubCRResource;

public class ContextInitListener implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		// force HubCR init.
		HubCRResource.getManager();
	}

}
