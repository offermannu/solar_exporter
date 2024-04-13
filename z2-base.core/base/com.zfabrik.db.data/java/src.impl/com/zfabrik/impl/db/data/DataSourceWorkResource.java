/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.db.data;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.impl.db.data.DataSourceResource.Enlistment;
import com.zfabrik.util.function.ThrowingSupplier;
import com.zfabrik.work.IWorkResource;
import com.zfabrik.work.WorkException;
import com.zfabrik.work.WorkUnit;

/**
 * Work resource, keeping the connection per data source on the work unit.
 */
public class DataSourceWorkResource implements IWorkResource {
	private final static Logger LOG = Logger.getLogger(DataSourceWorkResource.class.getName());
	private DataSourceResource dataSourceResource;
	private Connection conn;
	private Connection wrapper;

	public DataSourceWorkResource(DataSourceResource dataSourceResource, final Connection conn) throws Exception {
		this.dataSourceResource = dataSourceResource;
		this.conn = conn;
		
		// overwrite the close method. 
		this.wrapper = (Connection) Proxy.newProxyInstance(
			this.getClass().getClassLoader(), 
			new Class<?>[]{Connection.class}, 
			(Object proxy, Method method, Object[] args) -> {
				if (!"close".equals(method.getName())) {
					return method.invoke(DataSourceWorkResource.this.conn, args);
				} else {
					// if anybody set the connection to auto commit, 
					// we set it back to no-autocommit
					if (conn.getAutoCommit()) {
						LOG.warning("Setting pool connection back to non-autocommit");
						conn.setAutoCommit(false);
					}
				}
				return null;
			}
		);
	}
	
	/**
	 * Get a work resource from the work unit. Bind a new one, if none present
	 */
	public static DataSourceWorkResource get(DataSourceResource dataSourceResource, ThrowingSupplier<Connection, SQLException> supplier) {
		String dataSourceName = dataSourceResource.getName();
		String key = computeWorkUnitResourceKey(dataSourceName);
		IWorkResource wr = WorkUnit.getCurrent().getResource(key);
		DataSourceWorkResource dswu;
		if (wr != null) {
			return (DataSourceWorkResource) wr;
		} else {
			try {
				dswu = new DataSourceWorkResource(
					dataSourceResource,
					supplier.get()
				);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new IllegalStateException("Failed to initialize work resource: "+ dataSourceName, ex);
			}
			try {
				WorkUnit.getCurrent().bindResource(key, dswu);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IllegalStateException("Failed to bind resource to work unit: "+ dataSourceName, e);
			}
			return dswu;
		}
	}

	public static String computeWorkUnitResourceKey(String dataSourceName) {
		return DataSourceResource.class.getName() + ":"+ dataSourceName;
	}
	
	/**
	 * Unbind this {@link DataSourceResource} from the work unit. This is used to unbind and close
	 * after commit and rollback in the case of JTA enlistment (i.e. we unbind, because the transaction
	 * unbinds after commit/rollback). See #2077
	 */
	private void unbind() {
		WorkUnit wu = WorkUnit.queryCurrent();
		if (wu!=null) {
			String key = computeWorkUnitResourceKey(this.dataSourceResource.getName());
			IWorkResource currently = wu.getResource(key);
			if (currently==this) {
				wu.unbindResource(key);
			} else {
				throw new IllegalStateException("Found unexpected resource "+currently+" bound at key "+key);
			}
		}
	}
	
	public Connection getConnection() {
		if (this.wrapper == null) {
			throw new IllegalStateException(
				"Found stale work resource (no connection anymore) at " + 
				computeWorkUnitResourceKey(this.dataSourceResource.getName())
			);
		}
		return this.wrapper;
	}

	public void begin() {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Begin connection \""+this.wrapper+"\" : "+dataSourceResource.getName());
		}
		try {
			this.conn.setAutoCommit(false);
		} catch (SQLException e) {
			throw new WorkException("Work unit begin failed: "+dataSourceResource.getName());
		}
	}

	public void close() throws WorkException {
		try {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Close connection \""+this.wrapper+"\": "+dataSourceResource.getName());
			}
			this.conn.close();
		} catch (SQLException e) {
			throw new WorkException("Error during close of connection: " + dataSourceResource.getName(), e);
		} finally {
			this.conn = null;
			this.wrapper = null;
		}
	}

	public void commit() {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Commit connection \""+this.wrapper+"\" : "+dataSourceResource.getName());
		}
		if (this.conn != null) {
			try {
				if (!this.conn.isClosed()) {
					// #2071: In case the connection was set to autocommit, do not commit!
					if (this.conn.getAutoCommit()) {
						LOG.warning(this.conn + " was set to autocommit before commit - skipping commit");
					} else {
						this.conn.commit();
					}
				}
			} catch (SQLException e) {
				throw new WorkException("Error during commit: " + dataSourceResource.getName(), e);
			}
		}
		if (dataSourceResource.getEnlistment()==Enlistment.jta) {
			// The transaction will be unbound. Unbind as well (this will imply close)
			this.unbind();
		}
	}

	public void rollback() throws WorkException {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.finer("Rollback connection \""+this.wrapper+"\" : "+dataSourceResource.getName());
		}
		if (this.conn != null) {
			try {
				if (!this.conn.isClosed()) {
					// #2071: In case the connection was set to autocommit, do not roll back!
					if (this.conn.getAutoCommit()) {
						LOG.warning(this.conn + " was set to autocommit before commit - skipping rollback");
					} else {
						this.conn.rollback();
					}
				}
			} catch (SQLException e) {
				throw new WorkException("Error during rollback: " + dataSourceResource.getName(), e);
			}
		}
		if (dataSourceResource.getEnlistment()==Enlistment.jta) {
			// The transaction will be unbound. Unbind as well (this will imply close)
			this.unbind();
		}
	}

}