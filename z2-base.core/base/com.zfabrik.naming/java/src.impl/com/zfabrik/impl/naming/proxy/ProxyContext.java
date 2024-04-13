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

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

/**
 * Just a proxy to another context
 * 
 * @author Henning
 * 
 */
public class ProxyContext implements Context {
	@SuppressWarnings("unused")
	private ProxyNamingProvider.Config cfg;
	private Context targetContext;

	public ProxyContext(ProxyNamingProvider.Config cfg, Context targetContext) {
		this.cfg = cfg;
		this.targetContext = targetContext;
	}

	public Object addToEnvironment(String propName, Object propVal) throws NamingException {
		return targetContext.addToEnvironment(propName, propVal);
	}

	public void bind(Name name, Object obj) throws NamingException {
		targetContext.bind(name, obj);
	}

	public void bind(String name, Object obj) throws NamingException {
		targetContext.bind(name, obj);
	}

	public void close() throws NamingException {
		targetContext.close();
	}

	public Name composeName(Name name, Name prefix) throws NamingException {
		return targetContext.composeName(name, prefix);
	}

	public String composeName(String name, String prefix) throws NamingException {
		return targetContext.composeName(name, prefix);
	}

	public Context createSubcontext(Name name) throws NamingException {
		return targetContext.createSubcontext(name);
	}

	public Context createSubcontext(String name) throws NamingException {
		return targetContext.createSubcontext(name);
	}

	public void destroySubcontext(Name name) throws NamingException {
		targetContext.destroySubcontext(name);
	}

	public void destroySubcontext(String name) throws NamingException {
		targetContext.destroySubcontext(name);
	}

	public Hashtable<?, ?> getEnvironment() throws NamingException {
		return targetContext.getEnvironment();
	}

	public String getNameInNamespace() throws NamingException {
		return targetContext.getNameInNamespace();
	}

	public NameParser getNameParser(Name name) throws NamingException {
		return targetContext.getNameParser(name);
	}

	public NameParser getNameParser(String name) throws NamingException {
		return targetContext.getNameParser(name);
	}

	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		return targetContext.list(name);
	}

	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return targetContext.list(name);
	}

	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		return targetContext.listBindings(name);
	}

	public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
		return targetContext.listBindings(name);
	}

	public Object lookup(Name name) throws NamingException {
		return targetContext.lookup(name);
	}

	public Object lookup(String name) throws NamingException {
		return targetContext.lookup(name);
	}

	public Object lookupLink(Name name) throws NamingException {
		return targetContext.lookupLink(name);
	}

	public Object lookupLink(String name) throws NamingException {
		return targetContext.lookupLink(name);
	}

	public void rebind(Name name, Object obj) throws NamingException {
		targetContext.rebind(name, obj);
	}

	public void rebind(String name, Object obj) throws NamingException {
		targetContext.rebind(name, obj);
	}

	public Object removeFromEnvironment(String propName) throws NamingException {
		return targetContext.removeFromEnvironment(propName);
	}

	public void rename(Name oldName, Name newName) throws NamingException {
		targetContext.rename(oldName, newName);
	}

	public void rename(String oldName, String newName) throws NamingException {
		targetContext.rename(oldName, newName);
	}

	public void unbind(Name name) throws NamingException {
		targetContext.unbind(name);
	}

	public void unbind(String name) throws NamingException {
		targetContext.unbind(name);
	}

}
