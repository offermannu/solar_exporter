/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.naming;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.impl.naming.components.NamingProviderResource;
import com.zfabrik.naming.jndi.provider.INamingProvider;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.expression.X;
import com.zfabrik.util.internal.NamingProvisioningHolder;

/**
 * Start provide hook into naming system for our own naming extensions.
 * Z2 naming extensions are made available via {@link NamingBuilderHolder}
 * 
 * @author HB
 */
public class NamingSystemHook extends Resource {

	/**
	 * @see sample.service.IService#init(IServiceContext)
	 */
	public void init() {
		try {
			NamingProvisioningHolder.INSTANCE.setNaming(
					new NamingProvisioningHolder.NamingProvisioning() {
						public InitialContextFactory findInitialContextFactory(String initCtxtFactoryName, Hashtable<?, ?> env) throws NamingException {
							return NamingSystemHook.findInitialContextFactory(initCtxtFactoryName, env);
						}
						public ObjectFactory findURLContextFactory(String scheme, Hashtable<?, ?> env) throws NamingException {
							return NamingSystemHook.findURLContextFactory(scheme, env);
						}
					}
			);
		} catch (Exception e) {
			throw new IllegalStateException("Initialization of Naming Feature failed", e);
		}
	}

	public <T> T as(Class<T> clz) {
		return null;
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		NamingProvisioningHolder.INSTANCE.setNaming(null);
	}

	//
	// utilities
	//

	/**
	 * finds an initial context factory
	 * 
	 * @param initCtxtFactoryName
	 * @param env
	 * @return
	 * @throws NamingException
	 */
	public static InitialContextFactory findInitialContextFactory(String initCtxtFactoryName, Hashtable<?, ?> env) throws NamingException {
		INamingProvider prov = _findProvider(NamingProviderResource.ICFACTORIES, initCtxtFactoryName);
		if (prov != null) {
			return prov.getInitialContextFactory(env);
		} else {
			// default JNDI behavior:
			try {
				return (InitialContextFactory) Class.forName(initCtxtFactoryName, false, Thread.currentThread().getContextClassLoader()).newInstance();
			} catch (VirtualMachineError vme) {
				throw vme;
			} catch (Throwable e) {
				NamingException ne = new NamingException("Failed to load initial context factory " + initCtxtFactoryName + ": " + e);
				ne.setRootCause(e);
				throw ne;
			}
		}

	}

	/**
	 * Method findURLContextFactory.
	 * 
	 * @param scheme
	 * @param env
	 * @return ObjectFactory
	 */
	public static ObjectFactory findURLContextFactory(String scheme, Hashtable<?, ?> env) throws NamingException {
		INamingProvider prov = _findProvider(NamingProviderResource.SCHEMES, scheme);
		if (prov != null) {
			return prov.getURLContextFactory(scheme, env);
		}
		throw new NamingException("Failed to find URL context factory for scheme "+scheme);
	}

	/*
	 * find provider in naming
	 */
	private static INamingProvider _findProvider(String name, String value) {
		try {
			X x = X.var(IComponentDescriptor.COMPONENT_TYPE).eq(X.val("com.zfabrik.naming.jndi.provider")).and(X.val(value).in(X.var(name)));
			Collection<String> cs = IComponentsManager.INSTANCE.findComponents(x);
			if (cs.size() == 0) {
				return null;
			} else {
				String cn = cs.iterator().next();
				return IComponentsLookup.INSTANCE.lookup(cn, INamingProvider.class);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to retrieve naming provider for (name,value)=(" + name + "," + value + ")");
		}
	}

}
