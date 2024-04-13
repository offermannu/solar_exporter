/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.naming.proxy;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

import com.zfabrik.naming.jndi.provider.util.AbstractURLContextFactory;

/**
 * @author Henning
 * 
 */
public class ProxyURLContextFactory extends AbstractURLContextFactory {
	private String scheme;
	private ProxyNamingProvider.Config cfg;

	public ProxyURLContextFactory(ProxyNamingProvider.Config cfg, String scheme) {
		this.cfg = cfg;
		this.scheme = scheme;
	}

	protected Context createURLContext(Hashtable<?, ?> env) throws NamingException {
		return new ProxyURLContext(this.cfg, this.scheme, env);
	}

}
