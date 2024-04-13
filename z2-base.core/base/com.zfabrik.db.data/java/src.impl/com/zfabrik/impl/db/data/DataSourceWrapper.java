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

import static com.zfabrik.impl.db.data.DataSourceResource.getMethodIfAvailable;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;
import javax.transaction.Status;

import com.zfabrik.tx.UserTransaction;
import com.zfabrik.util.threading.ThreadUtil;
import com.zfabrik.work.WorkUnit;

/**
 * Wraps a nested data source implementation so that we can hook up with the {@link WorkUnit}.
 */
public class DataSourceWrapper implements DataSource {
	private final static Logger LOG = Logger.getLogger(DataSourceWrapper.class.getName());
	private final DataSourceResource resource;
	private DataSource lds;
	private Method getParentLogger;

	public DataSourceWrapper(DataSourceResource dataSourceResource, DataSource dataSource) {
		this.resource = dataSourceResource;
		this.lds = dataSource;
		this.getParentLogger = getMethodIfAvailable(this.lds.getClass(),"getParentLogger");
	}
	
	public Connection getConnection() throws SQLException {
		/**
		 * See http://bugs.mysql.com/bug.php?id=36565
		 */
		return ThreadUtil.cleanContextExecute(()->{
			switch (resource.getEnlistment()) {
				case jta:  {
					Connection c = null;
					if (UserTransaction.get().getStatus()!=Status.STATUS_NO_TRANSACTION) {
						// we have a tx
						// get/bind from workunit
						c = getFromWorkUnitIfPresent(resource.getName());
					}
					if (c == null) {
						LOG.fine("Data source "+resource.getName()+" is configured for enlisting in JTA transaction but no work unit was found. Will return unenlisted connection");
						c  = lds.getConnection();
					}
					return c;
				}
				case workUnit: {
					Connection c = getFromWorkUnitIfPresent(resource.getName());
					if (c == null) {
						LOG.fine("Data source "+resource.getName()+" is configured for enlisting in work unit but no work unit was found. Will return unenlisted connection");
						c  = lds.getConnection();
					}
					return c;
				}
				case none:
				default:
					return lds.getConnection();
			}
		});
	}

	/**
	 * Get a connection from a work unit if present, and bind the resource. Returns null otherwise 
	 */
	private Connection getFromWorkUnitIfPresent(String dataSourceName) throws SQLException {
		WorkUnit wu = WorkUnit.queryCurrent();
		if (wu!=null) { 
			return DataSourceWorkResource.get(resource, ()->lds.getConnection()).getConnection();
		} else {
			return null;
		}
	}

	public Connection getConnection(String username, String password) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return false;
	}

	public <T> T unwrap(Class<T> iface) throws SQLException {
		return null;
	}

	public int getLoginTimeout() throws SQLException {
		return this.lds.getLoginTimeout();
	}
	
	public PrintWriter getLogWriter() throws SQLException {
		return this.lds.getLogWriter();
	}

	public void setLoginTimeout(int seconds) throws SQLException {
		this.lds.setLoginTimeout(seconds);
	}

	public void setLogWriter(PrintWriter out) throws SQLException {
		this.lds.setLogWriter(out);
	}

	void _kill() {
		resource._destroy(lds);
	}
	
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return DataSourceResource.getParentLogger(this.lds, this.getParentLogger);
	}
}