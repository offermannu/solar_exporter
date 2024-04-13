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
import java.util.Properties;

import javax.naming.CompoundName;
import javax.naming.Name;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingException;

import com.zfabrik.naming.jndi.provider.util.AbstractContext;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.IResourceLookup;

public class RMNamespaceContext extends AbstractContext {
	private static Properties SYNTAX = new Properties();
	private IResourceLookup namespace;
	private String name;
	
	public static final NameParser nameParser = new NameParser() {

		public Name parse(String name) throws NamingException {
			return new CompoundName(name,SYNTAX);
		}
	};


	public RMNamespaceContext(Hashtable<?, ?> env, IResourceLookup namespace, String name) {
		super(env);
		this.namespace = namespace;
		this.name = name;
	}

	public NameParser doGetNameParser(String name) throws NamingException {
		return nameParser;
	}

	/*
	 * retrieve the resource
	 */
	protected Object doLookup(String localName) throws NamingException {
		String as = null;
		int p = localName.lastIndexOf('?');
		if (p>=0) {
			// parse the query string
			String qs = localName.substring(p+1);
			String[] sp = qs.split("&");
			for (String x : sp) {
				if (x.startsWith("type=")) {
					as = x.substring(5);
				}
			}
			localName = localName.substring(0,p);
		}
		IResourceHandle rh = this.namespace.lookup(localName, IResourceHandle.class);
		Class<?> clz = null;
		if (as!=null) {
			try {
				clz = Class.forName(as,false,Thread.currentThread().getContextClassLoader());
			} catch (Exception e) {
				NamingException ex = new NamingException("Failed to find type specifier class in resource lookup ("+localName+" on "+rh.getResourceInfo().getName()+")");
				ex.initCause(e);
				throw ex;
			}
		} else {
			clz = Object.class;
		}
		Object o = rh.as(clz);
		if (o==null) 
			throw new NameNotFoundException(localName);
		return o;
	}

	public String getNameInNamespace() throws NamingException {
		return this.name;
	}

}

