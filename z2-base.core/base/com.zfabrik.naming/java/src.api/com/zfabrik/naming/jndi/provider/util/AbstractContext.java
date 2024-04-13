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

import javax.naming.Binding;
import javax.naming.CannotProceedException;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;
import javax.naming.Referenceable;
import javax.naming.spi.NamingManager;

/**
 * Helper JNDI context. Reduces necessary operations to 
 * methods that only work on the local naming scheme (see the protected methods)
 * 
 * @author HB
 */

public abstract class AbstractContext implements Context {
	protected Hashtable<Object, Object> env;

	@SuppressWarnings("unchecked")
	public AbstractContext(Hashtable<?, ?> env) {
		this.env = (Hashtable<Object, Object>) env.clone();
	}

	/**
	 * access to continuation context
	 */
	protected Context getContinuationContext(Name n) throws NamingException {
		Object obj = doContinuationContextLookup(n.get(0));
		CannotProceedException cpe = new CannotProceedException();
		cpe.setResolvedObj(obj);
		cpe.setEnvironment(this.env);
		return NamingManager.getContinuationContext(cpe);
	}

	/**
	 * prepares an object for binding by resolving state transitions and
	 * Referencable etc.
	 * <p>
	 * <b>Note:</b> requires implementation of doGetNameParser in order to
	 * retrieve a good local name representation
	 * 
	 * @param name
	 * @param obj
	 * @return a prepared object for binding
	 * @throws NamingException
	 */
	protected Object prepareState(String name, Object obj) throws NamingException {
		Object result = NamingManager.getStateToBind(obj, doGetNameParser(name).parse(name), this, env);
		// Check for Referenceable
		if (result instanceof Referenceable) {
			result = ((Referenceable) result).getReference();
		}
		return result;
	}

	/**
	 * @see javax.naming.Context#lookup(Name)
	 */
	public Object lookup(Name name) throws NamingException {
		if (name.isEmpty()) {
			return doLookup("");
		} else if (name.size() == 1) {
			return doLookup(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.lookup(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * lookup of continuation context. 
	 * NOTE: usually this is crucial to avoid applications of object factories during
	 * continuation lookup!!
	 */
	protected Context doContinuationContextLookup(String name) throws NamingException {
		// default to normal lookup
		Object result = doLookup(name);
		try {
			return (Context) result;
		} catch (ClassCastException cce) {
			throw new IllegalStateException(this + ": expecting context at " + name);
		}
	}

	/**
	 * overwrite to provide lookup (local name)
	 */
	protected Object doLookup(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#lookup(String)
	 */
	public Object lookup(String name) throws NamingException {
		return this.lookup(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#bind(Name, Object)
	 */
	public void bind(Name name, Object obj) throws NamingException {
		if (name.size() == 1) {
			doBind(name.get(0), obj);
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.bind(name.getSuffix(1), obj);
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * BindOperation
	 */
	protected void doBind(String name, Object obj) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#bind(String, Object)
	 */
	public void bind(String name, Object obj) throws NamingException {
		this.bind(doGetNameParser(name).parse(name), obj);
	}

	/**
	 * @see javax.naming.Context#rebind(Name, Object)
	 */
	public void rebind(Name name, Object obj) throws NamingException {
		if (name.size() == 1) {
			doRebind(name.get(0), obj);
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.rebind(name.getSuffix(1), obj);
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * BindOperation
	 */
	protected void doRebind(String name, Object obj) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#rebind(String, Object)
	 */
	public void rebind(String name, Object obj) throws NamingException {
		this.rebind(doGetNameParser(name).parse(name), obj);
	}

	/**
	 * @see javax.naming.Context#unbind(Name)
	 */
	public void unbind(Name name) throws NamingException {
		if (name.size() == 1) {
			doUnbind(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.unbind(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * Unbind
	 */
	protected void doUnbind(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#unbind(String)
	 */
	public void unbind(String name) throws NamingException {
		this.unbind(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#rename(Name, Name)
	 */
	public void rename(Name oldName, Name newName) throws NamingException {
		if (oldName.size() == 1) {
			if (newName.size() != 1) {
				throw new OperationNotSupportedException("Renaming to a Name with more components not supported: " + newName);
			}
			doRename(oldName.get(0), newName.get(0));
		} else {
			if (!oldName.get(0).equals(newName.get(0))) {
				throw new OperationNotSupportedException("Renaming using different non-last components not supported: " + oldName + " " + newName);
			}
			Context ctx = getContinuationContext(oldName);
			try {
				ctx.rename(oldName.getSuffix(1), newName.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * Rename
	 */
	protected void doRename(String oldName, String newName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#rename(String, String)
	 */
	public void rename(String oldName, String newName) throws NamingException {
		this.rename(doGetNameParser(oldName).parse(oldName), doGetNameParser(newName).parse(newName));
	}

	/**
	 * @see javax.naming.Context#list(Name)
	 */
	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		if (name.size() == 0) {
			return doList("");
		} else if (name.size() == 1) {
			return doList(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.list(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * List
	 */
	protected NamingEnumeration<NameClassPair> doList(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#list(String)
	 */
	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return this.list(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#listBindings(Name)
	 */
	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		if (name.isEmpty()) {
			return doListBindings("");
		}
		if (name.size() == 1) {
			return doListBindings(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.listBindings(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	/**
	 * local List Bindings
	 */
	protected NamingEnumeration<Binding> doListBindings(String localName) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#listBindings(String)
	 */
	public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
		return this.listBindings(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#destroySubcontext(Name)
	 */
	public void destroySubcontext(Name name) throws NamingException {
		if (name.size() == 1) {
			doDestroySubcontext(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.destroySubcontext(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public void doDestroySubcontext(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#destroySubcontext(String)
	 */
	public void destroySubcontext(String name) throws NamingException {
		this.destroySubcontext(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#createSubcontext(Name)
	 */
	public Context createSubcontext(Name name) throws NamingException {
		if (name.size() == 1) {
			return doCreateSubcontext(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.createSubcontext(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public Context doCreateSubcontext(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#createSubcontext(String)
	 */
	public Context createSubcontext(String name) throws NamingException {
		return this.createSubcontext(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#lookupLink(Name)
	 */
	public Object lookupLink(Name name) throws NamingException {
		if (name.size() == 1) {
			return doLookupLink(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.lookupLink(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public Object doLookupLink(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#lookupLink(String)
	 */
	public Object lookupLink(String name) throws NamingException {
		return this.lookupLink(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#getNameParser(Name)
	 */
	public NameParser getNameParser(Name name) throws NamingException {
		if (name.size() == 1) {
			return doGetNameParser(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.getNameParser(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public NameParser doGetNameParser(String name) throws NamingException {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see javax.naming.Context#getNameParser(String)
	 */
	public NameParser getNameParser(String name) throws NamingException {
		return this.getNameParser(doGetNameParser(name).parse(name));
	}

	/**
	 * @see javax.naming.Context#composeName(Name, Name)
	 */
	public Name composeName(Name name, Name prefix) throws NamingException {
		Name result = (Name) prefix.clone();
		result.addAll(name);
		return result;
	}

	/**
	 * @see javax.naming.Context#composeName(String, String)
	 */
	public String composeName(String name, String prefix) throws NamingException {
		return this.composeName(doGetNameParser(name).parse(name),doGetNameParser(prefix).parse(prefix)).toString();
	}

	/**
	 * @see javax.naming.Context#addToEnvironment(String, Object)
	 */
	public Object addToEnvironment(String propName, Object propVal) throws NamingException {
		return this.env.put(propName, propVal);
	}

	/**
	 * @see javax.naming.Context#removeFromEnvironment(String)
	 */
	public Object removeFromEnvironment(String propName) throws NamingException {
		return this.env.remove(propName);
	}

	/**
	 * @see javax.naming.Context#getEnvironment()
	 */
	public Hashtable<?, ?> getEnvironment() throws NamingException {
		return this.env;
	}

	/**
	 * @see javax.naming.Context#close()
	 */
	public void close() throws NamingException {}
}
