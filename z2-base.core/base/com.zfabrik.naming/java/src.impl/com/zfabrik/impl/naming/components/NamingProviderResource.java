/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.naming.components;

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.StateFactory;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.impl.naming.proxy.ProxyNamingProvider;
import com.zfabrik.naming.jndi.provider.INamingProvider;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

/**
 * Entity Handle for Naming Provider
 * 
 * @author Henning Blohm
 */
public class NamingProviderResource extends Resource {

	/*
	 * TODO: introduce thread context awareness. a provider would say what
	 * context it applies to, while a thread would be contextualized in some
	 * try{}finally{} way. proposed notation: <scheme, factory>@<context> This
	 * would provide naming resolution per hosted environment, e.g. web
	 * container type/instance
	 */
	public final static String PROVIDER_TYPE = "naming.jndi.provider.type";

	public final static String PROVIDER_TYPE_IMPL = "provider";
	public final static String PROVIDER_TYPE_PROXY = "proxy";

	public final static String CLASSNAME = IComponentDescriptor.COMPONENT_CLZ;

	public final static String ICFACTORIES = "naming.jndi.provider.initialContextFactoryNames";
	public final static String OBJECT_FACTORIES = "naming.jndi.provider.factories.object";
	public final static String STATE_FACTORIES = "naming.jndi.provider.factories.state";

	public final static String SCHEMES = "naming.jndi.provider.schemes";

	//
	// proxy definitions
	//
	public final static String PROXY_ICF = "naming.jndi.proxy.initialContextFactory";
	public final static String PROXY_ROOT = "naming.jndi.proxy.root";
	public final static String PROXY_OBJECT_FACTORIES = "naming.jndi.proxy.factories.object";
	public final static String PROXY_STATE_FACTORIES = "naming.jndi.proxy.factories.state";

	private String name;
	private INamingProvider np = null;

	private class NP implements INamingProvider {
		private INamingProvider np;

		public NP(INamingProvider np) {
			this.np = np;
		}

		public InitialContextFactory getInitialContextFactory(
				Hashtable<?, ?> env) throws NamingException {
			return this.np.getInitialContextFactory(env);
		}

		public ObjectFactory getObjectFactory(String name)
				throws NamingException {
			return this.np.getObjectFactory(name);
		}

		public StateFactory getStateFactory(String name) throws NamingException {
			return this.np.getStateFactory(name);
		}

		public ObjectFactory getURLContextFactory(String scheme,
				Hashtable<?, ?> env) throws NamingException {
			return this.np.getURLContextFactory(scheme, env);
		}
	}

	/**
	 * Constructor NamingProviderHandle.
	 * 
	 * @param name
	 * @param appBroker
	 * @param entityBroker
	 */
	public NamingProviderResource(String name) {
		this.name = name;
	}

	public <T> T as(Class<T> clz) {
		if (INamingProvider.class.equals(clz)) {
			synchronized (this) {
				if (this.np == null) {
					IComponentDescriptor desc = IComponentsManager.INSTANCE
							.getComponent(name);
					if (desc != null) {
						Properties props = desc.getProperties();
						// check for the type of provider...
						String type = props.getProperty(PROVIDER_TYPE,
								PROVIDER_TYPE_IMPL);
						if (PROVIDER_TYPE_PROXY.equals(type)) {
							// its a proxy to another provider.
							this.np = new NP(new ProxyNamingProvider(this.name,
									desc));
						} else {
							// it's a real provider implementation
							this.np = new NP(JavaComponentUtil
									.loadImplementationFromJavaComponent(
											this.name,
											IComponentDescriptor.COMPONENT_CLZ,
											handle(),
											INamingProvider.class));
						}
					}
				}
				return clz.cast(this.np);
			}
		}
		return null;
	}

	@Override
	public void invalidate()
			throws ResourceBusyException {
		this.np = null;
	}

}
