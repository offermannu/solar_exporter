/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.tx;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.work.WorkUnit;

/**
 * simple user tx wrapper around our workunit
 * @author hb
 *
 */
public class UserTransactionImpl extends Resource implements UserTransaction, TransactionManager {
	
	/**
	 * Set <code>jta.log.classPrefixes</code> as comma-separated list of prefixes that the build-in trace
	 * will check for on call stacks to output a method name for method invocation on the transaction management.
	 * This is extremely useful for transaction demarcation debugging.  
	 */
	public static final String JTA_LOG_CLASS_PREFIXES = "jta.log.classPrefixes";
	
	private static String[] logPrefixes;
	
	
	public <T> T as(Class<T> clz) {
		load();
		if (Object.class.equals(clz)) {
			return clz.cast(this);
		} else
		if (UserTransaction.class.equals(clz)) {
			return clz.cast(this);
		} else
		if (TransactionManager.class.equals(clz)) {
			return clz.cast(this);
		}			
		return null;
	}
	
	private synchronized void load() {
		if (logPrefixes==null) {
			Properties p = handle().as(IComponentDescriptor.class).getProperties();
			String prs = p.getProperty(JTA_LOG_CLASS_PREFIXES,"").trim();
			StringTokenizer tk = new StringTokenizer(prs,",");
			logPrefixes = new String[tk.countTokens()];
			int i=0;
			while (tk.hasMoreTokens()) {
				logPrefixes[i] = tk.nextToken().trim();
			}
		}
	}

	@Override
	public synchronized void invalidate() throws ResourceBusyException {
		super.invalidate();
		logPrefixes=null;
	}

	public void begin() throws NotSupportedException, SystemException {
		try {
			// everything happens inside the Tx impl
			new TransactionImpl();
			_trace("begin",Level.FINE);
		} catch (Exception e) {
			SystemException se = new SystemException();
			se.initCause(e);
			throw se;
		}
	}

	public void commit() throws RollbackException, HeuristicMixedException,HeuristicRollbackException, SecurityException,	IllegalStateException, SystemException {
		TransactionImpl.get().commit();
	}

	public int getStatus() throws SystemException {
		TransactionImpl tx = TransactionImpl.query();
		if (tx==null) {
			_trace("getStatus (UT:STATUS_NO_TRANSACTION)",Level.FINER);
			return Status.STATUS_NO_TRANSACTION;
		}
		return tx.getStatus();
	}

	public void rollback() throws IllegalStateException, SecurityException,SystemException {
		TransactionImpl.get().rollback();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		TransactionImpl.get().setRollbackOnly();
	}

	public void setTransactionTimeout(int arg0) throws SystemException {
		// ignore
	}

	public Transaction getTransaction() throws SystemException {
		return TransactionImpl.query();
	}

	public void resume(Transaction tx) throws InvalidTransactionException,
			IllegalStateException, SystemException {
		WorkUnit.attach(((TransactionImpl)tx).getWu());
		_trace("resume",Level.FINE);
	}

	public Transaction suspend() throws SystemException {
		_trace("suspend",Level.FINE);
		TransactionImpl tx = TransactionImpl.query();
		if (tx!=null) {
			WorkUnit.detach();
		}
		return tx;
	}

	protected static void _trace(String string, Level level) {
		if (logger.isLoggable(level)) {
			String loc = null;
			String[] prs = logPrefixes;
			if (prs!=null && prs.length>0) {
				StackTraceElement[] stes = Thread.currentThread().getStackTrace();
				for (int i=0;i<stes.length && loc==null;i++) {
					StackTraceElement ste = stes[i];
					for (int j=0;j<prs.length && loc==null;j++) {
						if (ste.getClassName().startsWith(prs[j])) {
							loc=ste.getClassName()+"."+ste.getMethodName()+":"+ste.getLineNumber();
						}
					}
				}
			}
			logger.log(level,"User Transaction ("+WorkUnit.queryCurrent()+"): "+string+(loc!=null? " at "+loc:""));
		}
	}

	protected final static Logger logger = Logger.getLogger(UserTransactionImpl.class.getName());
	
}

