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

import com.zfabrik.naming.jndi.provider.util.AbstractURLContext;

/**
 * @author Henning
 * 
 */
public class ProxyURLContext extends AbstractURLContext implements Context {
	private ProxyNamingProvider.Config cfg;

	protected ProxyURLContext(ProxyNamingProvider.Config cfg, String scheme, Hashtable<?, ?> env) {
		super(scheme, env);
		this.cfg = cfg;
	}

	protected Context getRootContext(Hashtable<?, ?> env) throws NamingException {
		return new ProxyInitialContextFactory(this.cfg).getInitialContext(env);
	}
}
