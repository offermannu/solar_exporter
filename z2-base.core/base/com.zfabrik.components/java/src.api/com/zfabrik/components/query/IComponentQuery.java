/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.components.query;

import com.zfabrik.util.expression.X;

/**
 * A query resource can be associated with a query expression using this interface
 * @author hb
 *
 */
public interface IComponentQuery {

	/**
	 * set the query expression. This will override previous settings
	 * @param x
	 */
	void setX(X x);
	
	/**
	 * get the query expression
	 */
	X getX();
	
	/**
	 * check if the query expression returns the same result since it has been set.
	 */
	boolean verify();
	
}
