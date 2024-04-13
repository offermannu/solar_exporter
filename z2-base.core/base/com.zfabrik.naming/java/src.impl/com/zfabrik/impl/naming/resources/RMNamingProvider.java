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

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.StateFactory;

import com.zfabrik.naming.jndi.provider.INamingProvider;

public class RMNamingProvider implements INamingProvider {

	public InitialContextFactory getInitialContextFactory(Hashtable<?, ?> env) throws NamingException {
		return new RMInitialContextFactory();
	}

	public ObjectFactory getObjectFactory(String name) throws NamingException {
		return null;
	}

	public StateFactory getStateFactory(String name) throws NamingException {
		return null;
	}

	public ObjectFactory getURLContextFactory(String scheme, Hashtable<?, ?> env) throws NamingException {
		return new RMURLContextFactory();
	}

}
