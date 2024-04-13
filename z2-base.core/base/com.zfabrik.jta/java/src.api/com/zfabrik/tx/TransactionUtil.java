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

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import com.zfabrik.util.function.ThrowingRunnable;
import com.zfabrik.util.function.ThrowingSupplier;
import com.zfabrik.util.threading.ThreadUtil;

/**
 * This class provides utilities around transaction management with Z2's built-in JTA implementation.
 * 
 * @author hb
 *
 */
public class TransactionUtil {

	/**
	 * The TransactionUtilException is used to wrap system style exceptions possibly occuring, but normally
	 * not of interest to application, when invoking {@link TransactionUtil#get(UserTransaction, ThrowingSupplier)} or {@link TransactionUtil#get(UserTransaction, ThrowingSupplier)}
	 */
	public static class TransactionUtilException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public TransactionUtilException(Throwable cause) {
			super("System error during transaction util execution",cause);
		}
	}
	
	/**
	 * Use this method to execute work in a transaction. This is just a utility that helps implementing
	 * for example Transaction Servlet Filters that begin and end transactions at the request boundaries.
	 * <p>
	 * If there is an ongoing transaction already, this method will not begin a new one. Any exception caught in that case will
	 * set that new transaction to rollback. 
	 * <p>
	 * See also {@link ThreadUtil#confine(Callable, Class)} on how to handle checked exception cases
	 * gracefully.
	 * 
	 * @param ut the {@link UserTransaction} instance of the transaction management system in place
	 * @param callable The {@link Callable} implementation that defines the work.
	 * @return The result of the callable invocation.
	 * @throws Exception
	 */
	public static <V> V run(UserTransaction ut, Callable<V> callable) throws Exception {
		if (ut.getStatus()!=Status.STATUS_ACTIVE) {
			ut.begin();
			try {
				return callable.call();
			} catch (Exception e) {
				logger.log(Level.WARNING,"Error at transaction boundary. Setting transaction to rollbackonly",e);
				ut.setRollbackOnly();
				throw e;
			} finally {
				if (ut.getStatus()==Status.STATUS_ACTIVE) {
					ut.commit();
				} else {
					ut.rollback();
				}
			}
		} else {
			// there is a tx ongoing already. Simply work.
			return callable.call();
		}
	}

	/**
	 * Use this method to execute work in a transaction that returns a result. 
	 * <p>
	 * If there is an ongoing transaction already, this method will not begin a new one. Any exception caught in that case will
	 * set that new transaction to rollback. 
	 * <p>
	 * This method relies on {@link ThrowingSupplier} to give the caller control over what exceptions may be propagated.
	 * <p>
	 * Any other checked exceptions that may be thrown by underlying transaction management methods, such as {@link SystemException}, {@link NotSupportedException}
	 * will be wrapped into a {@link TransactionUtilException}.
	 * 
	 * @param ut the {@link UserTransaction} instance of the transaction management system in place
	 * @param supplier The {@link ThrowingSupplier} implementation that defines the work.
	 * @return The result of the supplier invocation.
	 * @throws Exception
	 */
	public static <V, E extends Exception> V get(UserTransaction ut, ThrowingSupplier<V, E> supplier) throws E, TransactionUtilException {
		int status;
		try {
			status = ut.getStatus();
		} catch (Exception syse) {
			throw new TransactionUtilException(syse);
		}
		if (status!=Status.STATUS_ACTIVE) {
			try {
				ut.begin();
			} catch (Exception be) {
				throw new TransactionUtilException(be);
			}
			boolean success=false;
			try {
				V v = supplier.get();
				success=true;
				return v;
			} finally {
				try {
					if (!success) {
						logger.log(Level.WARNING,"Error at transaction boundary. Setting transaction to rollbackonly");
						ut.setRollbackOnly();
					}
					if (ut.getStatus()==Status.STATUS_ACTIVE) {
						ut.commit();
					} else {
						ut.rollback();
					}
				} catch (Exception syse) {
					throw new TransactionUtilException(syse);
				}
			}
		} else {
			// there is a tx ongoing already. Simply work.
			return supplier.get();
		}
	}

	/**
	 * Use this method to execute work in a transaction that returns no result. 
	 * <p>
	 * If there is an ongoing transaction already, this method will not begin a new one. Any exception caught in that case will
	 * set that new transaction to rollback. 
	 * <p>
	 * This method relies on {@link ThrowingSupplier} to give the caller control over what exceptions may be propagated.
	 * <p>
	 * Any other checked exceptions that may be thrown by underlying transaction management methods, such as {@link SystemException}, {@link NotSupportedException}
	 * will be wrapped into a {@link TransactionUtilException}.
	 * 
	 * @param ut the {@link UserTransaction} instance of the transaction management system in place
	 * @param runnable The {@link ThrowingSupplier} implementation that defines the work.
	 * @return The result of the supplier invocation.
	 * @throws Exception
	 */
	public static <E extends Exception> void run(UserTransaction ut, ThrowingRunnable<E> runnable) throws E, TransactionUtilException {
		get(
			ut,
			()->{
				runnable.run();
				return null;
			}
		);
	}

	
	private final static Logger logger = Logger.getLogger(TransactionUtil.class.getName());
}
