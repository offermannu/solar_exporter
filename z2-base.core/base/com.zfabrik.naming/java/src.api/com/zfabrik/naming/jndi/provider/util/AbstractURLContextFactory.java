/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.naming.jndi.provider.util;

import java.util.Hashtable;

import javax.naming.ConfigurationException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.ObjectFactory;

/**
 * Helper URL Context Factory. 
 * 
 * @author HB
 */
public abstract class AbstractURLContextFactory implements ObjectFactory {

	/**
	 * @see javax.naming.spi.ObjectFactory#getObjectInstance(Object, Name,
	 *      Context, Hashtable)
	 */
	public Object getObjectInstance(Object urlInfo, Name name, Context nameCtx, Hashtable<?, ?> env) throws Exception {
		// Standard JNDI boilerplate code:
		// Case: urlInfo is null. 
		if (urlInfo == null) {
			return this.createURLContext(env);
		}

		// Case: urlInfo is a single string
		// Return the object named by urlInfo
		if (urlInfo instanceof String) {
			Context ctx = createURLContext(env);
			try {
				return ctx.lookup((String) urlInfo);
			} finally {
				ctx.close();
			}
		}

		// Case: urlInfo is an array of strings
		// Return the first one that responds w/o exception
		//
		if (urlInfo instanceof String[]) {
			String[] urls = (String[]) urlInfo;
			if (urls.length == 0) {
				throw new ConfigurationException(this.getClass().getName());
			}
			Context urlCtx = createURLContext(env);
			try {
				NamingException ne = null;
				for (int i = 0; i < urls.length; i++) {
					try {
						return urlCtx.lookup(urls[i]);
					} catch (NamingException e) {
						ne = e;
					}
				}
				throw ne;
			} finally {
				urlCtx.close();
			}
		}
		throw new IllegalArgumentException("urlInfo must be empty, a single string, or an array of strings");
	}

	/**
	 * Must provide a URL context that is capable 
	 */
	protected abstract Context createURLContext(Hashtable<?, ?> env) throws NamingException;

}
