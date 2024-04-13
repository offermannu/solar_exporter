/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;

import com.zfabrik.components.IDependencyComponent;

/**
 * JMX MBean interface of the home layout
 */
public interface HomeLayoutMBean {

	/**
	 * Prepare an arbitrary component. That is, if the component implements
	 * {@link IDependencyComponent}, the corresponding {@link IDependencyComponent#prepare()}
	 * method will be called. If the component does not exist or does not implement {@link IDependencyComponent}
	 * and {@link IllegalArgumentException} will be thrown.
	 * <p/>
	 * This method can be used, for example, to start a worker process or to attain
	 * a system state. 
	 */
	void prepare(String componentName) throws Exception;

}
