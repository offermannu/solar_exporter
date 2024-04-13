/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.tx;

import java.util.Arrays;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.junit.Assert;
import org.junit.Test;

import com.zfabrik.impl.tx.UserTransactionImpl;

/**
 * Unit tests for JTA support
 */
public class TransactionTests {

	/**
	 * Test that an error or exception thrown during before completion makes for a
	 * {@link RollbackException} and that the throwable is listed as cause
	 * @throws Exception
	 */
	@Test
	public void rollbackWithCauseDueToErrorInBeforeCompletion() throws Exception {
		Error error = new Error("Test");
		TransactionManager tm = new UserTransactionImpl();
		tm.begin();
		try {
			tm.getTransaction().registerSynchronization(new Synchronization() {

				@Override
				public void beforeCompletion() {
					throw error;
				}

				@Override
				public void afterCompletion(int arg0) {
				}
			});
			tm.commit();
			Assert.fail("Expected RollbackException");
		} catch (RollbackException rbe) {
			Assert.assertEquals(1, rbe.getSuppressed().length);
			Assert.assertTrue(rbe.getSuppressed()[0]==error);
			Assert.assertTrue(rbe.getCause()==error);
		}
	}

	/**
	 * Test that an multiple errors or exceptions thrown during before completion are conveyed
	 */
	@Test
	public void rollbackWithCausesDueToErrorInBeforeCompletion() throws Exception {
		Error error1 = new Error("Test1");
		Error error2 = new Error("Test2");
		TransactionManager tm = new UserTransactionImpl();
		tm.begin();
		Transaction tx = tm.getTransaction();
		try {
			tm.getTransaction().registerSynchronization(new Synchronization() {

				@Override
				public void beforeCompletion() {
					throw error1;
				}

				@Override
				public void afterCompletion(int arg0) {
				}
			});
			tm.getTransaction().registerSynchronization(new Synchronization() {

				@Override
				public void beforeCompletion() {
					try {
						Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK,tx.getStatus());
						Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK,tm.getStatus());
					} catch (SystemException se) {
						throw new RuntimeException(se);
					}
					throw error2;
				}

				@Override
				public void afterCompletion(int arg0) {
				}
			});
			tm.commit();
			Assert.fail("Expected RollbackException");
		} catch (RollbackException rbe) {
			Assert.assertEquals(2, rbe.getSuppressed().length);
			Assert.assertTrue(Arrays.equals(rbe.getSuppressed(),new Throwable[]{error1,error2}));
			Assert.assertTrue(rbe.getCause()==error1);
		}
	}

	/**
	 * See also https://docs.oracle.com/cd/E13222_01/wls/docs81/ConsoleHelp/jta.html, Table 54-2
	 * @throws Exception
	 */
	@Test
	public void statiCommitCase() throws Exception {
		TransactionManager tm = new UserTransactionImpl();
		Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
		tm.begin();
		Transaction tx = tm.getTransaction();
		Assert.assertEquals(Status.STATUS_ACTIVE,tm.getStatus());
		Assert.assertEquals(Status.STATUS_ACTIVE,tx.getStatus());
		tm.getTransaction().registerSynchronization(new Synchronization() {

			@Override
			public void beforeCompletion() {
				try {
					Assert.assertEquals(Status.STATUS_PREPARING,tm.getStatus());
					Assert.assertEquals(Status.STATUS_PREPARING,tx.getStatus());
				} catch (SystemException se) {
					throw new RuntimeException(se);
				}
			}

			@Override
			public void afterCompletion(int status) {
				try {
					Assert.assertEquals(Status.STATUS_COMMITTING,status);
					Assert.assertEquals(Status.STATUS_COMMITTING,tm.getStatus());
					Assert.assertEquals(Status.STATUS_COMMITTING,tx.getStatus());
				} catch (SystemException se) {
					throw new RuntimeException(se);
				}
			}
		});
		tm.commit();
		Assert.assertEquals(Status.STATUS_COMMITTED,tx.getStatus());
		// no tx on the transaction manager anymore
		Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
	}

	/**
	 * See also https://docs.oracle.com/cd/E13222_01/wls/docs81/ConsoleHelp/jta.html, Table 54-2
	 * @throws Exception
	 */
	@Test
	public void statiCommitRollBackOnly() throws Exception {
		TransactionManager tm = new UserTransactionImpl();
		Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
		tm.begin();
		Transaction tx = tm.getTransaction();
		Assert.assertEquals(Status.STATUS_ACTIVE,tm.getStatus());
		Assert.assertEquals(Status.STATUS_ACTIVE,tx.getStatus());

		tx.setRollbackOnly();
		Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK,tm.getStatus());
		Assert.assertEquals(Status.STATUS_MARKED_ROLLBACK,tx.getStatus());

		tm.getTransaction().registerSynchronization(new Synchronization() {

			@Override
			public void beforeCompletion() {
				Assert.fail();
			}

			@Override
			public void afterCompletion(int status) {
				try {
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,status);
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,tm.getStatus());
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,tx.getStatus());
				} catch (SystemException se) {
					throw new RuntimeException(se);
				}
			}
		});
		try {
			tm.commit();
			Assert.fail();
		} catch (RollbackException rbe) {
			Assert.assertEquals(Status.STATUS_ROLLEDBACK,tx.getStatus());
			// no tx on the transaction manager anymore
			Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
		}
	}

	/**
	 * See also https://docs.oracle.com/cd/E13222_01/wls/docs81/ConsoleHelp/jta.html, Table 54-2
	 * @throws Exception
	 */
	@Test
	public void statiCommitWithErrorInBeforeCompletion() throws Exception {
		TransactionManager tm = new UserTransactionImpl();
		Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
		tm.begin();
		Transaction tx = tm.getTransaction();
		Assert.assertEquals(Status.STATUS_ACTIVE,tm.getStatus());
		Assert.assertEquals(Status.STATUS_ACTIVE,tx.getStatus());
		tm.getTransaction().registerSynchronization(new Synchronization() {

			@Override
			public void beforeCompletion() {
				try {
					Assert.assertEquals(Status.STATUS_PREPARING,tm.getStatus());
					Assert.assertEquals(Status.STATUS_PREPARING,tx.getStatus());
				} catch (SystemException se) {
					throw new RuntimeException(se);
				}
				throw new RuntimeException("hello");
			}

			@Override
			public void afterCompletion(int status) {
				try {
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,status);
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,tm.getStatus());
					Assert.assertEquals(Status.STATUS_ROLLING_BACK,tx.getStatus());
				} catch (SystemException se) {
					throw new RuntimeException(se);
				}
			}
		});
		try {
			tm.commit();
			Assert.fail();
		} catch (RollbackException rbe) {
			Assert.assertEquals(Status.STATUS_ROLLEDBACK,tx.getStatus());
			// no tx on the transaction manager anymore
			Assert.assertEquals(Status.STATUS_NO_TRANSACTION,tm.getStatus());
		}
	}

	/**
	 * Test for #2012 checking that tx suspension returns null, if no tx is assocatied 
	 */
	@Test
	public void returnNullUponSuspensionOfnoTX() throws Exception {
		TransactionManager tm = new UserTransactionImpl();
		Assert.assertNull(tm.suspend());
	}


}
