/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.home;

import java.util.Map;

/**
 * A worker may have detached clones that are still alive.
 * This management interface provides access to all still managed worker 
 * processes for a given worker process component.
 * <p>
 * Worker processes have a fully qualified name of the form
 * <p>
 * <code>
 *  &lt;componentName&gt;@&lt;variant&gt;
 * </code>
 * <p>
 * The <code>variant</code> is also used for port customization by adding the variant number to the base port
 * config as defined via {@link IWorkerProcess#DEBUG_PORT} or {@link IWorkerProcess#JMX_PORT}.
 * 
 * @author hb
 *
 */
public interface IWorkerHorde {

	/**
	 * The worker definition component 
	 * @return
	 */
	String getComponentName();
	
	/**
	 * Get a map of all 
	 */
	
	/**
	 * Get the most current active worker process instance. Not the returned process may not 
	 * have been started yet. The active worker process is by definition a non-detached worker process.
	 * When detaching a worker process it becomes non-active. 
	 */
	IWorkerProcess getActiveWorkerProcess();
	
	/**
	 * Get the set of all worker process instances managed in this family.
	 * Not all of them may be running. And at most one is not in detached state.
	 * The returned map uses the fully qualified name as key.
	 */
	Map<String,IWorkerProcess> getWorkerProcesses();
	
}
