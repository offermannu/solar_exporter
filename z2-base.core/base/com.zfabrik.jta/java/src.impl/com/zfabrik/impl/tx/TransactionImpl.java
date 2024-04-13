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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

import com.zfabrik.work.IWorkResource;
import com.zfabrik.work.WorkUnit;

/**
 * Another wrapper around the work unit. Supports basic JTA 1.1 style usage
 *
 * @author hb
 *
 */
public class TransactionImpl implements Transaction {

	@SuppressWarnings("serial")
	public static Map<Integer,String> STATI = new HashMap<Integer, String>() {{
		put(Status.STATUS_ACTIVE,"STATUS_ACTIVE");
		put(Status.STATUS_COMMITTED,"STATUS_COMMITTED");
		put(Status.STATUS_COMMITTING,"STATUS_COMMITTING");
		put(Status.STATUS_MARKED_ROLLBACK,"STATUS_MARKED_ROLLBACK");
		put(Status.STATUS_NO_TRANSACTION,"STATUS_NO_TRANSACTION");
		put(Status.STATUS_PREPARED,"STATUS_PREPARED");
		put(Status.STATUS_PREPARING,"STATUS_PREPARING");
		put(Status.STATUS_ROLLEDBACK,"STATUS_ROLLEDBACK");
		put(Status.STATUS_ROLLING_BACK,"STATUS_ROLLING_BACK");
		put(Status.STATUS_UNKNOWN,"STATUS_UNKNOWN");
	}};

	private WorkUnit wu;
	private int status = Status.STATUS_ACTIVE;
	private final static String KEY = TransactionImpl.class.getName();
	private List<Synchronization> synchronizations;
	private boolean outer;

	// BEGIN Resource that represents the whole TX as WorkUnit resource.
	// to track changes and get the thread association
	public class Resource implements IWorkResource {
		// me memorize before completion rollback causes so we can add them to the
		// rollback exception we throw at unexpected rollback
		private List<Throwable> beforeCompletionRollbackCauses = new LinkedList<>();
		private boolean afterCompleted = false;

		public void beforeCompletion(boolean rollback) {
			UserTransactionImpl._trace("before completion",Level.FINER);
			// switch to preparing
			status = Status.STATUS_PREPARING;

			// handle synchronizations
			if (synchronizations!=null && !rollback) {
				for (Synchronization s : synchronizations) {
					try {
						UserTransactionImpl._trace("before completion: "+s,Level.FINER);
						s.beforeCompletion();
					} catch (Throwable e) {
						beforeCompletionRollbackCauses.add(e);
						UserTransactionImpl.logger.log(Level.SEVERE,"Exception in Tx synchronization ("+s+")",e);
						try {
							setRollbackOnly();
						} catch (Throwable re) {
							UserTransactionImpl.logger.log(Level.SEVERE,"Error during setRollbackOnly after beforeCompletion failure",re);
							if (re instanceof VirtualMachineError) {
								throw (VirtualMachineError) re;
							}
						}
						if (e instanceof VirtualMachineError) {
							throw (VirtualMachineError) e;
						}
					}
				}
			}

			// subsequently we are either rolling back or got into committing
			if (wu.getRollbackOnly()) {
				status = Status.STATUS_ROLLING_BACK;
			} else {
				status = Status.STATUS_COMMITTING;
			}
		}

		public List<Throwable> getBeforeCompletionRollbackCauses() {
			return beforeCompletionRollbackCauses;
		}

		public void begin() {};

		public void afterCompletion(boolean rollback) {
			if (!afterCompleted) {
				try {
					UserTransactionImpl._trace("after completion",Level.FINER);

					int current;
					try {
						current = getStatus();
					} catch (SystemException se) {
						throw new IllegalStateException("Caught error during afterCompletion",se);
					}
					if (synchronizations!=null) {
						for (Synchronization s : synchronizations) {
							try {
								s.afterCompletion(current);
							} catch (Exception e) {
								UserTransactionImpl.logger.log(Level.WARNING,"Exception in Tx synchronization ("+s+")",e);
							}
						}
					}
					// transition to next
					if (current == Status.STATUS_COMMITTING) {
						status = Status.STATUS_COMMITTED;
					} else if (current == Status.STATUS_ROLLING_BACK) {
						status = Status.STATUS_ROLLEDBACK;
					} else {
						status = Status.STATUS_UNKNOWN;
					}
				} finally {
					// Unfortunately Workunit will call us twice in case of rollback...
					afterCompleted = true;
				}
			}
		}

		public void close() {
			if (status==Status.STATUS_ACTIVE || status==Status.STATUS_MARKED_ROLLBACK) {
				throw new IllegalStateException("The JTA TX MUST be closed via the JTA Object before closing the work unit!");
			}
		}

		public TransactionImpl outer() {
			return TransactionImpl.this;
		}
	}

	// END Resource

	public TransactionImpl() {
		this.outer = WorkUnit.queryCurrent()==null;
		if (this.outer) {
			// we initialize here
			this.outer = true;
			WorkUnit.initCurrent();
		}
		this.wu = WorkUnit.queryCurrent();
		wu.bindResource(KEY, new Resource());
	}

	/**
	 * Accessor from current work unit. Returns null,  if no tx on work unit
	 * @return
	 */
	public static TransactionImpl query() {
		WorkUnit wu = WorkUnit.queryCurrent();
		if (wu != null) {
			Resource r = (Resource) wu.getResource(KEY);
			if (r!=null) {
				return r.outer();
			}
		}
		return null;
	}

	/**
	 * Accessor from current work unit. Throws Illegalstateexception, if no tx on work unit
	 * @return
	 */
	public static TransactionImpl get() {
		TransactionImpl tx = query();
		if (tx == null) {
			throw new IllegalStateException("No transaction associated with thread!");
		}
		return tx;
	}

	/**
	 * get underlying wu
	 * @return
	 */
	public WorkUnit getWu() {
		return wu;
	}

	/**
	 * Commit and detach. Rollback if required and throw RollbackException
	 */
	public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException,
			SystemException {

		UserTransactionImpl._trace("commit ("+(outer?"outer":"inner")+")", Level.FINE);
		int st = getStatus();
		if (st == Status.STATUS_ACTIVE || st == Status.STATUS_MARKED_ROLLBACK) {
			// note if we needed to roll back so that we can throw the roll back exception
			boolean rb = this.wu.getRollbackOnly();

			// if we are marked roll back, set to rolling back
			if (rb) {
				status = Status.STATUS_ROLLING_BACK;
			}

			// and keep the resource so check, if we had before completion problems
			Resource re = (Resource) wu.getResource(KEY);
			try {
				if (this.outer) {
					// if we are the outer boundary, we really release everything on a work unit level.
					this.wu.close();
				} else {
					// otherwise we only commit
					this.wu.commit();
				}
			} catch (Exception e) {
				SystemException se = new SystemException("JTA Tx commit failed ");
				se.initCause(e);
				throw se;
			} finally {
				// make sure we detach the transaction object
				this.wu.unbindResource(KEY);
			}
			if (rb) {
				// we had to rollback instead of commit
				throw new RollbackException();
			}
			if (!re.getBeforeCompletionRollbackCauses().isEmpty()) {
				// we rolled back due to a problem in before completion
				RollbackException rbe = new RollbackException();
				rbe.initCause(re.getBeforeCompletionRollbackCauses().get(0));
				for (Throwable t : re.getBeforeCompletionRollbackCauses()) {
					rbe.addSuppressed(t);
				}
				throw rbe;
			}

		} else
			throw new IllegalStateException("Transaction in status " + st + " cannot be committed");
	}

	/**
	 * Rollback and detach
	 */
	public void rollback() throws IllegalStateException, SystemException {
		UserTransactionImpl._trace("rollback ("+(outer?"outer":"inner")+")", Level.FINE);
		int st = getStatus();
		if (st == Status.STATUS_ACTIVE || st == Status.STATUS_MARKED_ROLLBACK) {
			this.wu.setRollbackOnly();
			try {
				if (this.outer) {
					// if we are the outer boundary, we really release everything on a work unit level.
					this.wu.close();
				} else {
					// otherwise we only commit (implies rollback)
					this.wu.commit();
				}
			} catch (Exception e) {
				SystemException se = new SystemException("JTA Tx commit failed ");
				se.initCause(e);
				throw se;
			} finally {
				// make sure we detach the transaction object
				this.wu.unbindResource(KEY);
			}
		} else
			throw new IllegalStateException("Transaction in status " + st + " cannot be committed");
	}

	/**
	 * return tx status
	 */
	public int getStatus() throws SystemException {
		if (this.status == Status.STATUS_ACTIVE && this.wu.getRollbackOnly()) {
			// note the rollback only in a deferred way - it may be set via the work unit
			this.status = Status.STATUS_MARKED_ROLLBACK;
		}
		UserTransactionImpl._trace("getStatus (TX:"+STATI.get(this.status)+")",Level.FINER);
		return this.status;
	}

	/**
	 * Mark Tx for rollback
	 */
	public void setRollbackOnly() throws IllegalStateException, SystemException {
		UserTransactionImpl._trace("setRollbackOnly",Level.FINE);
		this.status = Status.STATUS_MARKED_ROLLBACK;
		this.wu.setRollbackOnly();
	}


	/**
	 * Tx synchronization
	 */
	public synchronized void registerSynchronization(Synchronization sync) throws RollbackException, IllegalStateException, SystemException {
		UserTransactionImpl._trace("registerSynchronization ("+sync+")",Level.FINER);
		if (this.synchronizations==null) {
			this.synchronizations = new ArrayList<Synchronization>(10);
		}
		this.synchronizations.add(sync);
	}

	/**
	 * XA Resource enlisting. Not supported!
	 */
	public boolean delistResource(XAResource resource, int arg1) throws IllegalStateException, SystemException {
		throw new UnsupportedOperationException("not just yet");
	}

	/**
	 * XA Resource enlisting. Not supported!
	 */
	public boolean enlistResource(XAResource resource) throws RollbackException, IllegalStateException, SystemException {
		throw new UnsupportedOperationException("not just yet");
	}

}
