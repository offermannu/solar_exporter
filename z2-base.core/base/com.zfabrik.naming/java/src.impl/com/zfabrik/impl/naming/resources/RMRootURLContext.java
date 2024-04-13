/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.naming.resources;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

import com.zfabrik.naming.jndi.provider.util.AbstractURLContext;

public class RMRootURLContext extends AbstractURLContext implements Context {

	public RMRootURLContext(Hashtable<?, ?> env) {
		super("resources",env);
	}
	protected Context getRootContext(Hashtable<?, ?> env) throws NamingException {
		return new RMRootContext(env);
	}

}
