/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;

import com.zfabrik.gateway.IGatewayInfo;
import com.zfabrik.gateway.worker.GatewayServer;
import com.zfabrik.impl.gateway.home.GatewayHandlerImpl;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.workers.home.WorkerUtils;

/**
 * Public factory for internal implementation instances. Unfortunately we don't know how to use this also for the 
 * Server implementation {@link GatewayServer}. It is however used for implementations of the Gateway {@link Handler} and the {@link IGatewayInfo}. 
 */
public class GatewayFactoryImpl extends Resource implements IGatewayInfo {
	private Server server;
	private Handler handler;
	
	
	@Override
	public synchronized <T> T as(Class<T> clz) {
		if (Server.class.equals(clz)) {
			if (this.server == null) {
				this.server = new GatewayServer();
			}
			return clz.cast(this.server);
		}
		if (Handler.class.equals(clz)) {
			if (this.handler == null) {
				this.handler = new GatewayHandlerImpl();
			}
			return clz.cast(this.handler);
		}
		if (IGatewayInfo.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}
	
	// gate way state
	@Override
	public String getTargetWorkerName() {
		try {
			return (String) _getMbs().getAttribute(
				GatewayHandlerImpl.getMBeanObjectName(), 
				"TargetWorkerProcessName"
			);
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve MBean attribute",e);
		}
	}

	@Override
	public int getGatewayPort(String workerProcessName) {
        return GatewayHandlerImpl.getGatewayBasePort(WorkerUtils.getWorkerComponentName(workerProcessName))
        		  +Integer.parseInt(workerProcessName.substring(workerProcessName.lastIndexOf('@')+1));

	}
	
	private IHomeMBeanServer _getMbs() {
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		return mbs;
	}

}
