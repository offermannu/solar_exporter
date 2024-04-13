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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.StateFactory;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.impl.naming.components.NamingProviderResource;
import com.zfabrik.naming.jndi.provider.INamingProvider;
import com.zfabrik.resources.IResourceHandle;

/**
 * @author Henning
 * 
 */
public class ProxyNamingProvider implements INamingProvider {

	protected static class Config {
		public String objectFactories;
		public String stateFactories;
		public String icf;
		public String root;
	}

	private Properties props;
	private String name;
	private Config cfg = new Config();
	private Map<String, Class<?>> objectFactories = null;
	private Map<String, Class<?>> stateFactories = null;

	/**
	 * @param props
	 */
	public ProxyNamingProvider(String name, IComponentDescriptor desc) {
		Map<String, Class<?>> tm = new HashMap<String, Class<?>>();
		this.props = desc.getProperties();
		try {
			this.cfg.objectFactories = _parse(props.getProperty(NamingProviderResource.PROXY_OBJECT_FACTORIES), tm);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse object factories specification for proxy provider " + name, e);
		}
		this.objectFactories = tm;
		tm = new HashMap<String, Class<?>>();
		try {
			this.cfg.stateFactories = _parse(props.getProperty(NamingProviderResource.PROXY_STATE_FACTORIES), tm);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to parse state factories specification for proxy provider " + name, e);
		}
		this.stateFactories = tm;
		this.cfg.root = props.getProperty(NamingProviderResource.PROXY_ROOT);
		this.cfg.icf = props.getProperty(NamingProviderResource.PROXY_ICF);
		if ((this.cfg.root == null) && (this.cfg.icf == null)) {
			throw new IllegalArgumentException("Proxy: need specification of initial context factory or target root");
		}
	}

	private String _parse(String in, Map<String, Class<?>> map) throws Exception {
		if (in == null)
			return null;
		StringTokenizer tk = new StringTokenizer(in, ":");
		StringBuffer result = new StringBuffer(in.length());
		String o, o2, cn;
		while (tk.hasMoreTokens()) {
			o = tk.nextToken();
			int p = o.lastIndexOf('(');
			if (p >= 0) {
				// this object factory is annotated by a class name
				int q = o.indexOf(')', p);
				if (q < 0)
					throw new IllegalStateException("configuration error: entity property for object/state factory contains opening brace but misses closing brace");
				o2 = o.substring(0, p);
				cn = o.substring(p + 1, q);
				IResourceHandle rh = JavaComponentUtil.getJavaComponent(this.name);
				IJavaComponent jc = rh.as(IJavaComponent.class);
				if (jc==null)
					throw new IllegalStateException("failed to retrieve java component for component: "+this.name);
				map.put(o2, Class.forName(cn, false, jc.getPrivateLoader()));
				o = o2;
			}
			result.append(o);
			if (tk.hasMoreTokens())
				result.append(':');
		}
		return result.toString();
	}

	public InitialContextFactory getInitialContextFactory(Hashtable<?, ?> env) throws NamingException {
		return new ProxyInitialContextFactory(this.cfg);
	}

	/**
	 * @see com.zfabrik.naming.jndi.INamingProvider#getURLContextFactory(java.lang.String,
	 *      java.util.Hashtable)
	 */
	public ObjectFactory getURLContextFactory(String scheme, Hashtable<?, ?> env) throws NamingException {
		return new ProxyURLContextFactory(this.cfg, scheme);
	}

	/**
	 * @see com.zfabrik.naming.jndi.INamingProvider#getObjectFactory(java.lang.String)
	 */
	public ObjectFactory getObjectFactory(String name) throws NamingException {
		if (this.objectFactories != null) {
			try {
				return (ObjectFactory) ((Class<?>) this.objectFactories.get(name)).newInstance();
			} catch (Exception e) {
				NamingException ne = new NamingException("failed to instantiate object factory");
				ne.setRootCause(e);
				throw ne;
			}
		}
		return null;
	}

	/**
	 * @see com.zfabrik.naming.jndi.INamingProvider#getStateFactory(java.lang.String)
	 */
	public StateFactory getStateFactory(String name) throws NamingException {
		if (this.stateFactories != null) {
			try {
				return (StateFactory) ((Class<?>) this.stateFactories.get(name)).newInstance();
			} catch (Exception e) {
				NamingException ne = new NamingException("failed to instantiate state factory");
				ne.setRootCause(e);
				throw ne;
			}
		}
		return null;
	}

	protected final static Logger logger = Logger.getLogger("system.naming.proxy");
}
