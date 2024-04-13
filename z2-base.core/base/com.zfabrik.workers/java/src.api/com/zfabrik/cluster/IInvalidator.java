/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.cluster;

import java.util.Collection;
import java.util.Set;

import com.zfabrik.workers.home.IHomeLayout;
import com.zfabrik.workers.worker.HomeHandle;

/**
 * Client interface for invalidators. Invalidator components offer this interface. Typically invalidators are property-declared components
 * that require no coding but rely on a generic implementation. There can be more that one invalidator in use at a time. The default invalidator 
 * is declared in the home layout. Usually there is no need to look it up but rather invalidations can be posted via the home handle {@link HomeHandle}. 
 * @author hb
 *
 */
public interface IInvalidator {
	String TYPE 			 = "com.zfabrik.cluster.invalidator";
	String INVALIDATOR_ID      			 = "invalidator.id";
	String INVALIDATOR_ID_DEFAULT = "default";
	
	
	/**
	 * post a set of resource invalidations into a clustered environment 
	 * Note: these will not be effective for the current home.
	 * See also {@link HomeHandle#broadcastInvalidations(Set, short)} and {@link IHomeLayout#broadcastInvalidations(Set, long, short) }  
	 * on the current VM.
	 * @param invs
	 */
	void postInvalidations(Collection<String> invs);
}
