/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.gateway.worker;

import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.util.annotation.Name;

/**
 * We need a special connection factory so we can provide our own connection implementation {@link GatewayConnection}.
*/
public class GatewayConnectionFactory extends HttpConnectionFactory {
	
	public GatewayConnectionFactory() {
		super();
	}

	public GatewayConnectionFactory(@Name("config") HttpConfiguration config) {
		super(config);
	}

	@Override
	public Connection newConnection(Connector connector, EndPoint endPoint) {
        return configure(new GatewayConnection(getHttpConfiguration(), connector, endPoint), connector, endPoint);
	}
	
}
