/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util;

/**
 * a simple handler - style interface that can be used as a callback
 * during iteration over some set of objects
 * 
 * @author hb
 *
 */
public interface Visitor<T> {

	/**
	 * if returning <code>true</code> continue visiting, otherwise stop.
	 * @param t
	 * @return
	 */
	boolean visit(T t);
	
}
