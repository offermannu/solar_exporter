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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * @author Henning
 * 
 */
public class ProxyInitialContextFactory implements InitialContextFactory {
	private ProxyNamingProvider.Config cfg;

	protected ProxyInitialContextFactory(ProxyNamingProvider.Config cfg) {
		this.cfg = cfg;
	}

	@SuppressWarnings("unchecked")
	public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
		// find the root from the target provider
		Hashtable<Object, Object> env = (Hashtable<Object, Object>) environment.clone();
		// reset initial context factory setting
		if (this.cfg.icf!=null) {
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("setting initial_context_factory to \"" + this.cfg.icf + "\"");
			}
			env.put(Context.INITIAL_CONTEXT_FACTORY, this.cfg.icf);
		}
		//
		// any aspect setting deferred until we have the real root...
		//
		Context ctx = new InitialContext(env);
		String root = this.cfg.root;
		if (root == null)
			root = "";
		if (logger.isLoggable(Level.FINER)) {
			logger.finer("will lookup target initial context at \"" + root + "\"");
		}
		Object obj = ctx.lookup(root);
		if (!(obj instanceof Context)) {
			throw new NamingException("proxy: target root lookup does not yield context but instead: " + obj);
		}
		ctx = (Context) obj;

		// inject object factory specifications
		if (this.cfg.objectFactories != null) {
			String ofo = (String) env.get(Context.OBJECT_FACTORIES);
			StringBuffer of = new StringBuffer(this.cfg.objectFactories.length() + (ofo != null ? ofo.length() : 0) + 1);
			of.append(this.cfg.objectFactories);
			if ((ofo != null) && (ofo.length() > 0)) {
				of.append(':');
				of.append(ofo);
			}
			ctx.removeFromEnvironment(Context.OBJECT_FACTORIES);
			ctx.addToEnvironment(Context.OBJECT_FACTORIES, of.toString());
			if (logger.isLoggable(Level.FINER)) {
				logger.finer("setting object_factories to \"" + of.toString() + "\"");
			}
		}

		// inject state factory specifications
		// CURRENTLY no support for custom state factories
		
//		if (this.cfg.stateFactories != null) {
//			String ofo = (String) ctx.getEnvironment().get(Context.STATE_FACTORIES);
//			if (ofo != null) {
//				if (logger.isLoggable(Level.FINER)) {
//					logger.finer("will not set state factories since already set to \"" + ofo + "\"");
//				}
//			} else {
//				ctx.addToEnvironment(Context.STATE_FACTORIES, DispatchStateFactory.class.getName());
//				ofo = (String) env.get(DispatchStateFactory.EXTRA_STATE_FACTORIES);
//				StringBuffer of = new StringBuffer(this.cfg.stateFactories.length() + (ofo != null ? ofo.length() : 0) + 1);
//				of.append(this.cfg.stateFactories);
//				if ((ofo != null) && (ofo.length() > 0)) {
//					of.append(':');
//					of.append(ofo);
//				}
//				ctx.addToEnvironment(DispatchStateFactory.EXTRA_STATE_FACTORIES, of.toString());
//				if (logger.isLoggable(Level.FINER)) {
//					logger.finer("setting state factories to \"" + of.toString() + "\"");
//				}
//			}
//		}
		return new ProxyContext(cfg, ctx);
	}

	private final static Logger logger = ProxyNamingProvider.logger;
}
