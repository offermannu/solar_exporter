/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.worker;

/**
 * The worker lease is a life-time self-control interface
 * for worker nodes.
 * <p>
 * Functionality on the worker node may increase or decrease
 * the worker node's lease counter. 
 * A worker node that has a zero lease counter and is detached
 * may terminate itself at any time.
 * <p>
 *  
 * @author hb
 *
 */
public interface IWorkerLease {

	void increaseLease();
	
	void decreaseLease();
	
}
