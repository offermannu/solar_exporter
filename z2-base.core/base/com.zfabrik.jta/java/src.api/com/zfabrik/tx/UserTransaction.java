/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.tx;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import com.zfabrik.components.IComponentsLookup;

/**
 * public wrapper of UT implementation
 */
public class UserTransaction implements javax.transaction.UserTransaction {
	/**
	 * Component name
	 */
	private static final String USER_TRANSACTION = "com.zfabrik.jta/userTransaction";

	public static javax.transaction.UserTransaction get() {
		return IComponentsLookup.INSTANCE.lookup(USER_TRANSACTION, javax.transaction.UserTransaction.class);
	}
	
	public void begin() throws NotSupportedException, SystemException {
		get().begin();
	}

	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException,
			SystemException {
		get().commit();
	}

	public int getStatus() throws SystemException {
		return get().getStatus();
	}

	public void rollback() throws IllegalStateException, SecurityException, SystemException {
		get().rollback();
	}

	public void setRollbackOnly() throws IllegalStateException, SystemException {
		get().setRollbackOnly();
	}

	public void setTransactionTimeout(int arg0) throws SystemException {
		get().setTransactionTimeout(arg0);
	}

}
