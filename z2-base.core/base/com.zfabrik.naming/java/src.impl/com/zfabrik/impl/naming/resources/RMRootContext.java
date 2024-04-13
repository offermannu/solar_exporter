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

import javax.naming.Binding;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import com.zfabrik.naming.jndi.provider.util.AbstractContext;
import com.zfabrik.resources.IResourceLookup;
import com.zfabrik.resources.IResourceManager;

public class RMRootContext extends AbstractContext implements Context {
	public static final NameParser nameParser = new RMRootNameParser();

	// begin class Store Name Parser
	//
	private static class RMRootNameParser implements NameParser {
		static Properties SYNTAX = new Properties();
		static {
			SYNTAX.setProperty("jndi.syntax.direction", "left_to_right");
			SYNTAX.setProperty("jndi.syntax.separator", "/");
			SYNTAX.setProperty("jndi.syntax.ignorecase", "false");
			SYNTAX.setProperty("jndi.syntax.escape", "\\");
			SYNTAX.setProperty("jndi.syntax.trimblanks", "false");
		}

		public Name parse(String name) throws NamingException {
			int p = name.indexOf('/');
			if (p>=0) {
				String pref = name.substring(0,p+1);
				String suff = name.substring(p+1).replace("/","\\/");
				return new CompoundName(pref+suff,SYNTAX);
			} else {
				return new CompoundName(name,SYNTAX);
			}
		}
	}

	//

	public RMRootContext(Hashtable<?, ?> environment) {
		super(environment);
	}

	protected void doBind(String localName, Object obj) throws NamingException {
		throw new UnsupportedOperationException();
	}

	public Context doCreateSubcontext(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	public void doDestroySubcontext(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	public NameParser doGetNameParser(String name) throws NamingException {
		return nameParser;
	}

	protected NamingEnumeration<NameClassPair> doList(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	protected NamingEnumeration<Binding> doListBindings(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	protected Object doLookup(String localName) throws NamingException {
		IResourceLookup l = IResourceManager.INSTANCE.lookup(localName, IResourceLookup.class);
		if (l == null) {
//			logger.log(Level.WARNING,"Resource namespace not found: "+localName);
			throw new NameNotFoundException(localName);
		}
		return new RMNamespaceContext(getEnvironment(), l, localName);
	}

	public Object doLookupLink(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	protected void doRebind(String localName, Object obj) throws NamingException {
		throw new UnsupportedOperationException();
	}

	protected void doRename(String oldName, String newName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	protected void doUnbind(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	public void close() throws NamingException {
	}

	public String getNameInNamespace() throws NamingException {
		return "";
	}

//	private final static Logger logger = Logger.getLogger(RMRootContext.class.getName());
}
