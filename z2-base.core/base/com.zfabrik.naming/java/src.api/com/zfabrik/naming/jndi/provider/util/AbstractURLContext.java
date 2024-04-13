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
import javax.naming.spi.NamingManager;

/**
 * Helper URL context implementation that helps to implement a specific one over a root context
 * 
 * @author HB
 */
public abstract class AbstractURLContext implements Context {
	protected Hashtable<Object,Object> env;
	private String  scheme;
	private String  start;
	private Context rootContext = null;

	@SuppressWarnings("unchecked")
	protected AbstractURLContext(String scheme, Hashtable<?, ?> env) {
		this.env = (Hashtable<Object,Object>) env; // only used to get to wrapped context
		this.scheme = scheme;
		this.start  = scheme+":";
	}

	/**
	 * Must be overwritten to provide a root context that is
	 * wrapped by this URL context
	 */
	protected abstract Context getRootContext(Hashtable<?, ?> env) throws NamingException;

	/**
	 * internal wrapper to call getRootContext only if necessary
	 */
	private Context _getRootContext() throws NamingException {
		if (rootContext == null) {
			rootContext = getRootContext(env);
		}
		return rootContext;
	}

	/**
	 * gets the remaining Name after evaluation of the scheme prefix
	 */
	private String _getRemainingName(String url) throws NamingException {
		if (!url.startsWith(this.start)) {
			throw new IllegalArgumentException(url + " is not a " + scheme + " URL");
		}
		if (url.length() > this.start.length()) {
			return url.substring(this.start.length());
		} else {
			return "";
		}
	}

	/**
	 * get continuation context for URLs
	 */
	protected Context getContinuationContext(Name n) throws NamingException {
		Object obj = lookup(n.get(0));
		CannotProceedException cpe = new CannotProceedException();
		cpe.setResolvedObj(obj);
		cpe.setEnvironment(this.env);
		return NamingManager.getContinuationContext(cpe);
	}

	public Object lookup(String name) throws NamingException {
		return _getRootContext().lookup(_getRemainingName(name));
	}

	public Object lookup(Name name) throws NamingException {
		if (name.size() == 1) {
			return lookup(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.lookup(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public void bind(String name, Object obj) throws NamingException {
		_getRootContext().bind(_getRemainingName(name), obj);
	}

	public void bind(Name name, Object obj) throws NamingException {
		if (name.size() == 1) {
			bind(name.get(0), obj);
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.bind(name.getSuffix(1), obj);
			} finally {
				ctx.close();
			}
		}
	}

	public void rebind(String name, Object obj) throws NamingException {
		_getRootContext().rebind(_getRemainingName(name), obj);
	}

	public void rebind(Name name, Object obj) throws NamingException {
		if (name.size() == 1) {
			rebind(name.get(0), obj);
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.rebind(name.getSuffix(1), obj);
			} finally {
				ctx.close();
			}
		}
	}

	public void unbind(String name) throws NamingException {
		_getRootContext().unbind(_getRemainingName(name));
	}

	public void unbind(Name name) throws NamingException {
		if (name.size() == 1) {
			unbind(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.unbind(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	private String _getURLPrefix(String url) {
		int p = url.indexOf(':');
		if (p < 0) {
			throw new IllegalArgumentException("not a valid url " + url);
		}
		return url.substring(0, p);
	}

	public void rename(String oldName, String newName) throws NamingException {
		String oldPrefix = _getURLPrefix(oldName);
		String newPrefix = _getURLPrefix(newName);
		if (!oldPrefix.equals(newPrefix)) {
			throw new OperationNotSupportedException("Cannot rename across schemes (" + oldName + ", " + newName+")");
		}
		_getRootContext().rename(_getRemainingName(oldName), _getRemainingName(newName));
	}

	public void rename(Name name, Name newName) throws NamingException {
		if (name.size() == 1) {
			if (newName.size() != 1) {
				throw new OperationNotSupportedException("Cannot rename: " + newName);
			}
			rename(name.get(0), newName.get(0));
		} else {
			if (!name.get(0).equals(newName.get(0))) {
				throw new OperationNotSupportedException("Cannot rename on inner path (" + name + ", " + newName+")");
			}
			Context ctx = getContinuationContext(name);
			try {
				ctx.rename(name.getSuffix(1), newName.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
		return _getRootContext().list(_getRemainingName(name));
	}

	public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
		if (name.size() == 1) {
			return list(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.list(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
		return _getRootContext().listBindings(_getRemainingName(name));
	}

	public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
		if (name.size() == 1) {
			return listBindings(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.listBindings(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public void destroySubcontext(String name) throws NamingException {
		_getRootContext().destroySubcontext(_getRemainingName(name));
	}

	public void destroySubcontext(Name name) throws NamingException {
		if (name.size() == 1) {
			destroySubcontext(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				ctx.destroySubcontext(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public Context createSubcontext(String name) throws NamingException {
		return _getRootContext().createSubcontext(_getRemainingName(name));
	}

	public Context createSubcontext(Name name) throws NamingException {
		if (name.size() == 1) {
			return createSubcontext(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.createSubcontext(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public Object lookupLink(String name) throws NamingException {
		return _getRootContext().lookupLink(_getRemainingName(name));
	}

	public Object lookupLink(Name name) throws NamingException {
		if (name.size() == 1) {
			return lookupLink(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.lookupLink(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public NameParser getNameParser(String name) throws NamingException {
		return _getRootContext().getNameParser(_getRemainingName(name));
	}

	public NameParser getNameParser(Name name) throws NamingException {
		if (name.size() == 1) {
			return getNameParser(name.get(0));
		} else {
			Context ctx = getContinuationContext(name);
			try {
				return ctx.getNameParser(name.getSuffix(1));
			} finally {
				ctx.close();
			}
		}
	}

	public String composeName(String name, String prefix) throws NamingException {
		if (prefix.equals("")) {
			return name;
		} else if (name.equals("")) {
			return prefix;
		} else {
			return (prefix + "/" + name);
		}
	}

	public Name composeName(Name name, Name prefix) throws NamingException {
		Name result = (Name) prefix.clone();
		result.addAll(name);
		return result;
	}

	public String getNameInNamespace() throws NamingException {
		return _getRootContext().getNameInNamespace();
	}

	public Object removeFromEnvironment(String propName) throws NamingException {
		return _getRootContext().removeFromEnvironment(propName);
	}

	public Object addToEnvironment(String propName, Object propVal) throws NamingException {
		return _getRootContext().addToEnvironment(propName, propVal);
	}

	public Hashtable<?,?> getEnvironment() throws NamingException {
		return _getRootContext().getEnvironment();
	}

	public void close() throws NamingException {
		if (this.rootContext != null) {
			this.rootContext.close();
		}
	}
}