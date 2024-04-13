/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.gateway;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;

import com.zfabrik.components.IComponentsLookup;

/**
 * Utility factory to provide access to implementation level instances. Unfortunately the Server
 * instance is referenced by Jetty by class name so that it is pointless to provide the server 
 * instance from here.
 */
public class GatewayFactory {
	
	private static final String COMPONENT = "com.zfabrik.gateway/factory";

	public static Connector getConnector() {
		return IComponentsLookup.INSTANCE.lookup(COMPONENT, Connector.class);
	}

	public static Handler getHandler() {
		return IComponentsLookup.INSTANCE.lookup(COMPONENT, Handler.class);
	}
	
	public static IGatewayInfo getInfo() {
		return IComponentsLookup.INSTANCE.lookup(COMPONENT, IGatewayInfo.class);
	}
}
