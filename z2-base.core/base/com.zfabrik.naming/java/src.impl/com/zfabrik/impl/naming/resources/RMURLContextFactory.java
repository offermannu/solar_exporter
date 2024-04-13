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

import com.zfabrik.naming.jndi.provider.util.AbstractURLContextFactory;

public class RMURLContextFactory extends AbstractURLContextFactory {

	protected Context createURLContext(Hashtable<?,?> env) throws NamingException {
		return new RMRootURLContext(env);
	}

}
