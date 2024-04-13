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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.sql.DataSource;

import com.zfabrik.management.MBeanHub;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.util.threading.TimerUtil;

/**
 * A simple but effective data base connection pool. This pool manages a number of spare and a number of maximum connections.
 * Connections that have errors will be closed. Connections will be forcibly closed after some expiration period (no stale connections) 
 *  
 * <p>
 * Configuration properties:
 * <p>
 * <dl>
 * <dt>maxInUseConnection</dt><dd>Maximum number of connections provided by this pool</dd>
 * <dt>maxSpareConnection</dt><dd>Maximum number of spare connections</dd>
 * <dt>connectionExpiration</dt><dd>Connections expire after this many milli-seconds</dd>
 * <dt>connectionMaxUse</dt><dd>Connections expire after they have been checked out this many times</dd>
 * <dt>transactionIsolation</dt><dd>If set, all connections provided will be set to this isolation level. Use the transaction isolation constants on {@link Connection}.</dd>
 * <dt>driverClass</dt><dd>Set this, if you want the pool to use a driver class rather than using connections from a data source. The latter is 
 * is recommended. See {@link DataSourceResource} on how to specify a data source class.</dd>
 * <dt>user</dt><dd>If configuring the pool via a driver class, this user name will be used to request new connections.</dd>
 * <dt>password</dt><dd>If configuring the pool via a driver class, this password will be used to request new connections.</dd>
 * 
 * </dl>
 * 
 * @author hb
 *
 */
public class PoolingDataSource implements DataSource {
    // base config
    private String url;
    private String user;
    private String password;
    private int transactionIsolation = -1;

    // this pool may wrap another datasource rather than going by driver class.
    private DataSource dataSource;
    //
    // connections expire after this many milli-seconds
    private int connectionExpiration;
    // connections expire after they have been checked out this many times
    private int connectionMaxUse;
    
    // maximum number of connections provided by this pool
    private int maxInUseConnection;
    // maximum number of spare connections
    private int maxSpareConnection;
    //
    private ConnectionHolder[] spare;
    private ConnectionHolder[] inuse;
    //
    private int topSpare = 0, topInUse = 0;
    //
    private String name;
    private boolean destroyed = false;
    //
    // JMX
    private long jmxNoConnectionsCreated;
    private long jmxNoConnectionsServed;
    private long jmxMaxConcurrentConnections;
    private long jmxExpiredConnections;
    //
    // timer for expiration handling
    //
    private Timer timer;
    private TimerTask task;
        
    public PoolingDataSource(IResourceHandle rh, String name, DataSource ds) {
        this.name = name;
        this.dataSource = ds;
        try {
            ObjectName on = ObjectName.getInstance("zfabrik:type=" + PoolingDataSource.class.getName() + ",name=" + name);
            MBeanHub.registerMBean(rh, new StandardMBean(new PoolMBeanImpl(),PoolMBean.class), on);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register pool mbean", e);
        }
        this.setMaxInUseConnections(10);
        this.setMaxSpareConnections(5);
        this.setConnectionExpiration(30000);
        this.setConnectionMaxUse(500);
   }

    public PoolingDataSource(IResourceHandle rh, String name) {
    	this(rh,name,null);
    }

    public synchronized void destroy() {
        if (!this.destroyed) {
            try {
                if (this.timer != null) {
                    this.timer.cancel();
                }
            } finally {
                this.destroyed = true;
                this.timer = null;
            }
        }
    }

    // -----------------------------------------------------
    // MBean

    public static interface PoolMBean {
        void reset();

        // stats
        long getNumConnectionsServed();
        long getNumConnectionsCreated();
        long getMaxConcurrentConnections();
        long getExpiredConnections();
        // current
        int getNumSpareConnections();
        int getNumInUseConnections();
        // config
        int getMaxSpareConnections();
        void setMaxSpareConnections(int mc);
        int getMaxInUseConnections();
        void setMaxInUseConnections(int mc);
        int getConnectionExpiration();
        void setConnectionExpiration(int exp);
        int getConnectionMaxUse();
        void setConnectionMaxUse(int max);
        
        int getTransactionIsolation();
    }

    private class PoolMBeanImpl implements PoolMBean {
        public long getMaxConcurrentConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.jmxMaxConcurrentConnections;
            }
        }

        public long getNumConnectionsServed() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.jmxNoConnectionsServed;
            }
        }

        public long getNumConnectionsCreated() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.jmxNoConnectionsCreated;
            }
        }

        public long getExpiredConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.jmxExpiredConnections;
            }
        }

        public int getNumInUseConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.topInUse;
            }
        }

        public int getNumSpareConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.topSpare;
            }
        }

        public int getMaxSpareConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.maxSpareConnection;
            }
        }

        public int getMaxInUseConnections() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.maxInUseConnection;
            }
        }

        public void reset() {
            synchronized (PoolingDataSource.this) {
                PoolingDataSource.this.jmxMaxConcurrentConnections = 0;
                PoolingDataSource.this.jmxNoConnectionsServed = 0;
                PoolingDataSource.this.jmxExpiredConnections = 0;
                PoolingDataSource.this.jmxNoConnectionsCreated=0;
            }
        }

        public void setMaxInUseConnections(int mc) {
            PoolingDataSource.this.setMaxInUseConnections(mc);
        }

        public void setMaxSpareConnections(int mc) {
            PoolingDataSource.this.setMaxSpareConnections(mc);
        }

        public int getConnectionExpiration() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.connectionExpiration;
            }
        }

        public void setConnectionExpiration(int exp) {
            PoolingDataSource.this.setConnectionExpiration(exp);
        }
        
        public int getConnectionMaxUse() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.connectionMaxUse;
            }
        }

        public void setConnectionMaxUse(int max) {
            PoolingDataSource.this.setConnectionMaxUse(max);
        }
        
        public int getTransactionIsolation() {
            synchronized (PoolingDataSource.this) {
                return PoolingDataSource.this.transactionIsolation;
            }
        }
    }

    // -----------------------------------------------------
    // the actual pool implementation

    private static class ConnectionHolder {
    	private long created;
        private int numberUsed;
        private ConnectionWrapper connection;
    }

    /**
     * get a connection from the spare connections and if that is not possible,
     * create a new one. If however, maxConnections many connections are already
     * in use, then wait until one was freed.
     *
     * @return
     */
    private synchronized Connection sgetConnection() {
        if (this.destroyed)
            throw new IllegalStateException("This pool (" + this.name + ") has been destroyed already");
        while (topInUse >= maxInUseConnection) {
            // must wait for slot(s) to free up
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        if (this.destroyed)
            throw new IllegalStateException("This pool (" + this.name + ") has been destroyed already");
        // ok, take one from the spare connections if possible
        _checkSpareConnections();
        ConnectionHolder ch;
        if (this.topSpare > 0) {
        	// pull from the top, as that is where we put the last connection returned. This makes sure we expire as many as possible!
            ch = this.spare[--this.topSpare];
            this.spare[this.topSpare]=null;
            if (logger.isLoggable(Level.FINER)) {
            	logger.finer("Providing database connection \""+ch.connection+"\"  from the pool: "+this.name);
            }
        } else {
            // there was no good spare connection
            ch = new ConnectionHolder();
            ch.created = System.currentTimeMillis();
            ch.connection = _createConnection();
            if (logger.isLoggable(Level.FINER)) {
            	logger.finer("Creating new database connection \""+ch.connection+"\"  as the pool was empty: "+this.name);
            }
        }
        // add to inUse
        this.inuse[this.topInUse++] = ch;
        if (this.topInUse > this.jmxMaxConcurrentConnections)
            this.jmxMaxConcurrentConnections = this.topInUse;
        this.jmxNoConnectionsServed++;

        if (logger.isLoggable(Level.FINER)) {
        	logger.finer("Currently {spare: "+this.topSpare+", inUse: "+this.topInUse+"}: "+this.name);
        }

        return ch.connection;
    }

    private ConnectionWrapper _createConnection() {
    	ClassLoader ol = Thread.currentThread().getContextClassLoader();
        try {
        	Thread.currentThread().setContextClassLoader(DataSourceResource.getClassLoader(this.name));
        	Connection connection;
        	// when we have a configured datasource, use that
        	if (this.dataSource!=null) {
        		connection = this.dataSource.getConnection();
        	} else {
        		connection = DriverManager.getConnection(this.url, this.user, this.password);
        	}
			ConnectionWrapper w = new ConnectionWrapper(JDBCTracer.treatThis(connection));
        	if (this.transactionIsolation>=0) {
        		w.setTransactionIsolation(this.transactionIsolation);
        	}
        	return w;       	
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to acquire database connection", e);
        } finally {
        	Thread.currentThread().setContextClassLoader(ol);
        	this.jmxNoConnectionsCreated++;
        }
    }

	/**
     * Check all spare connections and remove those that are expired. This may
     * give us new room to keep spare connections
     */
    private void _checkSpareConnections() {
        int k = 0;
        boolean expired = false;
        long now = System.currentTimeMillis();
        ConnectionHolder ch;
        for (int i = 0; i < this.topSpare; i++) {
            ch = this.spare[i];
            if ((this.destroyed) || (now - ch.created > this.connectionExpiration)) {
                this.jmxExpiredConnections++;
                try {
                    if (logger.isLoggable(Level.FINER)) {
                    	logger.finer("Expiring connection \""+ch.connection+"\" after "+(now - ch.created)+"ms: "+this.name);
                    }
                    expired = true;
                    ch.connection.dump();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Caught exception when closing expired connection", e);
                }
            } else {
                if (k != i) {
                    this.spare[k] = ch;
                }
                k++;
            }
        }
        // clean up
        for (int i=k; i<this.topSpare;i++) {
        	this.spare[i]=null;
        }
        // new fill level
        this.topSpare = k;
        if (expired) {
            if (logger.isLoggable(Level.FINER)) {
            	logger.finer("Currently {spare: "+this.topSpare+", inUse: "+this.topInUse+"}: "+this.name);
            }
        }
    }

    // return to the pool
    private synchronized void sreturnConnection(ConnectionWrapper conn,boolean dump) {
    	long now = System.currentTimeMillis();
        // find the entry in inUseConnections
        for (int i = 0; i < this.topInUse; i++) {
            ConnectionHolder ch = this.inuse[i];
            if (ch.connection == conn) {
                // fix the last use and put it into the spare connections -
                // unless they are full already
            	ch.numberUsed++;
            	try {
	                this._checkSpareConnections();
	                if (!dump && (!this.destroyed) && (this.topSpare < this.maxSpareConnection) && (ch.numberUsed<this.connectionMaxUse) && (now-ch.created<this.connectionExpiration) ) {
	                    if (logger.isLoggable(Level.FINER)) {
	                    	logger.finer("Returning connection \""+ch.connection+"\" into the pool: "+this.name);
	                    }
	                    this.spare[this.topSpare++] = ch;
	                } else {
	                	if (now-ch.created>=this.connectionExpiration) {
		                    if (logger.isLoggable(Level.FINER)) {
		                    	logger.finer("Expiring connection \""+ch.connection+"\" as it is "+(now-ch.created)+"ms old: "+this.name);
		                    }
	                		// count as expiration
	                		this.jmxExpiredConnections++;
	                	} else
	                	if (ch.numberUsed>=this.connectionMaxUse) {
		                    if (logger.isLoggable(Level.FINER)) {
		                    	logger.finer("Expiring connection \""+ch.connection+"\" as it has been used "+ch.numberUsed+" times: "+this.name);
		                    }
	                		// count as expiration
	                		this.jmxExpiredConnections++;
	                	} else
	                    if (logger.isLoggable(Level.FINER)) {
	                    	logger.finer("Destroying connection \""+ch.connection+"\" as the pool is full or we are shutting down: "+this.name);
	                    }
	                    // dump the connection
	                    try {
	                        ch.connection.dump();
	                    } catch (Exception e) {
	                        logger.log(Level.WARNING, "Caught exception when closing no longer needed connection", e);
	                    }
	                }
            	} finally {
	                // pack the array
	                this.topInUse--;
	                for (int j = i; j < this.topInUse; j++) {
	                    this.inuse[j] = this.inuse[j + 1];
	                }
	                this.inuse[this.topInUse]=null;
	                // in case some thread is waiting for a free connection, notify
	                this.notify();
            	}
                break;
            }
            if (i==this.topInUse)
            	throw new IllegalStateException("Connection returned that was not served from this pool");
        }
        if (logger.isLoggable(Level.FINER)) {
        	logger.finer("Currently {spare: "+this.topSpare+", inUse: "+this.topInUse+"}: "+this.name);
        }
    }

    // -----------------------------------------------------
    // connection wrapper, so that we have control over pool related operations
    //
    private class ConnectionWrapper implements Connection {
        private Connection wrapped;
        // when receiving an error during commit or rollback, we mark the connection as stale 
        // and do not move it back into the pool
        private boolean stale = false;
        
        // Java 6/7 compatibility. These methods are mandatory when running on Java 7 and not on the 
        // wrapped connection when on Java 6. So we need to use some reflection
        private Method abort, setNetworkTimeout, getNetworkTimeout, setSchema, getSchema;
        
        private void markStale() {
        	if (!this.stale) {
	        	logger.warning("Identified stale database connection: "+this);
	        	this.stale = true;
        	}
        }

        public ConnectionWrapper(Connection wrapped) {
            this.wrapped = wrapped;
            
            // java 6/7 compatibility
    		Class<?> clz = this.wrapped.getClass();
    		this.abort = getMethodIfAvailable(clz,"abort", Executor.class);
    		this.setNetworkTimeout = getMethodIfAvailable(clz,"setNetworkTimeout", Executor.class, Integer.class);
    		this.getNetworkTimeout = getMethodIfAvailable(clz,"getNetworkTimeout");
    		this.setSchema = getMethodIfAvailable(clz,"setSchema", String.class);
    		this.getSchema = getMethodIfAvailable(clz,"getSchema");
        }
        
        public String toString() {
        	if (this.wrapped!=null) {
        		return this.wrapped.toString();
        	}
        	return super.toString();
        }        
        
        /*
         * The only method we care about overriding. We trust that the
         * connection will not be used afterwards, or bad things may happen
         */
        public void close() throws SQLException {
    		PoolingDataSource.this.sreturnConnection(this,this.stale);
        }

        public void dump() throws SQLException {
            // now... close the underlying connection
            this.wrapped.close();
        }

        // now.. only wrappers
        public void clearWarnings() throws SQLException {
    		this.wrapped.clearWarnings();
        }

        public void commit() throws SQLException {
        	boolean s = false;
        	try {
        		this.wrapped.commit();
        		s=true;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
            return this.wrapped.createArrayOf(typeName, elements);
        }

        public Blob createBlob() throws SQLException {
            return this.wrapped.createBlob();
        }

        public Clob createClob() throws SQLException {
            return this.wrapped.createClob();
        }

        public NClob createNClob() throws SQLException {
            return this.wrapped.createNClob();
        }

        public SQLXML createSQLXML() throws SQLException {
            return this.wrapped.createSQLXML();
        }

        public Statement createStatement() throws SQLException {
        	boolean s = false;
        	try {
                Statement st = this.wrapped.createStatement();
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        	boolean s = false;
        	try {
                Statement st = this.wrapped.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        		s=true;
                return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        	boolean s = false;
        	try {
        		Statement st = this.wrapped.createStatement(resultSetType, resultSetConcurrency);
        		s=true;
                return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
            return this.wrapped.createStruct(typeName, attributes);
        }

        public boolean getAutoCommit() throws SQLException {
            return this.wrapped.getAutoCommit();
        }

        public String getCatalog() throws SQLException {
            return this.wrapped.getCatalog();
        }

        public Properties getClientInfo() throws SQLException {
            return this.wrapped.getClientInfo();
        }

        public String getClientInfo(String name) throws SQLException {
            return this.wrapped.getClientInfo(name);
        }

        public int getHoldability() throws SQLException {
            return this.wrapped.getHoldability();
        }

        public DatabaseMetaData getMetaData() throws SQLException {
        	boolean s = false;
        	try {
                DatabaseMetaData st = this.wrapped.getMetaData();
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public int getTransactionIsolation() throws SQLException {
            return this.wrapped.getTransactionIsolation();
        }

        public Map<String, Class<?>> getTypeMap() throws SQLException {
            return this.wrapped.getTypeMap();
        }

        public SQLWarning getWarnings() throws SQLException {
            return this.wrapped.getWarnings();
        }

        public boolean isClosed() throws SQLException {
            return this.wrapped.isClosed();
        }

        public boolean isReadOnly() throws SQLException {
            return this.wrapped.isReadOnly();
        }

        public boolean isValid(int timeout) throws SQLException {
            return this.wrapped.isValid(timeout);
        }

        public String nativeSQL(String sql) throws SQLException {
            return this.wrapped.nativeSQL(sql);
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        	boolean s = false;
        	try {
                CallableStatement st = this.wrapped.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        	boolean s = false;
        	try {
                CallableStatement st = this.wrapped.prepareCall(sql, resultSetType, resultSetConcurrency);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public CallableStatement prepareCall(String sql) throws SQLException {
        	boolean s = false;
        	try {
                CallableStatement st = this.wrapped.prepareCall(sql);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql, resultSetType, resultSetConcurrency);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql, autoGeneratedKeys);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql, columnIndexes);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql, columnNames);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public PreparedStatement prepareStatement(String sql) throws SQLException {
           	boolean s = false;
        	try {
                PreparedStatement st = this.wrapped.prepareStatement(sql);
        		s=true;
        		return st;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public void releaseSavepoint(Savepoint savepoint) throws SQLException {
            this.wrapped.releaseSavepoint(savepoint);
        }

        public void rollback() throws SQLException {
           	boolean s = false;
        	try {
                this.wrapped.rollback();
        		s=true;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public void rollback(Savepoint savepoint) throws SQLException {
           	boolean s = false;
        	try {
                this.wrapped.rollback(savepoint);
        		s=true;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public void setAutoCommit(boolean autoCommit) throws SQLException {
           	boolean s = false;
        	try {
                this.wrapped.setAutoCommit(autoCommit);
        		s=true;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public void setCatalog(String catalog) throws SQLException {
            this.wrapped.setCatalog(catalog);
        }

        public void setClientInfo(Properties properties) throws SQLClientInfoException {
            this.wrapped.setClientInfo(properties);
        }

        public void setClientInfo(String name, String value) throws SQLClientInfoException {
            this.wrapped.setClientInfo(name, value);
        }

        public void setHoldability(int holdability) throws SQLException {
            this.wrapped.setHoldability(holdability);
        }

        public void setReadOnly(boolean readOnly) throws SQLException {
            this.wrapped.setReadOnly(readOnly);
        }

        public Savepoint setSavepoint() throws SQLException {
            return this.wrapped.setSavepoint();
        }

        public Savepoint setSavepoint(String name) throws SQLException {
            return this.wrapped.setSavepoint(name);
        }

        public void setTransactionIsolation(int level) throws SQLException {
           	boolean s = false;
        	try {
                this.wrapped.setTransactionIsolation(level);
        		s=true;
        	} finally {
        		if (!s) { this.markStale(); };
        	}
        }

        public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
            this.wrapped.setTypeMap(map);
        }

        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }

        public <T> T unwrap(Class<T> iface) throws SQLException {
            return null;
        }
        
        private Object invoke7(Method method, Object ... args) throws SQLException {
        	if (method!=null) {
	    		try {
	        		return method.invoke(this.wrapped,args);
				} catch (InvocationTargetException e) {
					if (e.getCause() instanceof SQLException) {
						throw (SQLException) e.getCause();
					}
					throw new RuntimeException(e);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
        	}
        	throw new UnsupportedOperationException();
        }
        
        @SuppressWarnings("unused")
		public String getSchema() throws SQLException {
    		return (String) this.invoke7(this.getSchema);
        }
        
        @SuppressWarnings("unused")
        public void setSchema(String schema) throws SQLException {
        	this.invoke7(this.setSchema,schema);
        }

        @SuppressWarnings("unused")
        public int getNetworkTimeout() throws SQLException {
        	return (Integer) this.invoke7(this.getNetworkTimeout);
        }

        @SuppressWarnings("unused")
        public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        	this.invoke7(this.setNetworkTimeout, executor, milliseconds);
        }

        @SuppressWarnings("unused")
        public void abort(Executor executor) throws SQLException {
        	this.invoke7(this.abort, executor);
        }
    }

    // -----------------------------------------------------
    // data source
    //
    private PrintWriter out;

    public Connection getConnection() throws SQLException {
        return this.sgetConnection();
    }

    public Connection getConnection(String arg0, String arg1) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public synchronized PrintWriter getLogWriter() throws SQLException {
        return this.out;
    }

    public int getLoginTimeout() throws SQLException {
        return 0;
    }

    public synchronized void setLogWriter(PrintWriter out) throws SQLException {
        this.out = out;
    }

    public void setLoginTimeout(int arg0) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return false;
    }

    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return null;
    }

    // -----------------------------------
    // setup & config
    //

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setTransactionIsolation(int transactionIsolation) {
		this.transactionIsolation = transactionIsolation;
	}

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDriverClass(String driverClass) {
        // this.driverClass = driverClass;
        // make sure it gets initialized
    	ClassLoader cl = DataSourceResource.getClassLoader(this.name); // alternatively use context class loader?
        try {
            Class.forName(driverClass,true,cl);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load and initialize driver class (" + driverClass + ") using "+cl, e);
        }
    }
    
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    	return logger;
    }

    /**
     * Change the number of connections to be provided by this pool at most.
     * Request that would exceed this number have to wait for connections
     * to return. This setting can be changed at runtime. 
     * @param mc
     */
    public synchronized void setMaxInUseConnections(int mc) {
        this.maxInUseConnection = mc;
        if (this.inuse == null) {
            this.inuse = new ConnectionHolder[mc];
        } else {
            if (this.inuse.length < mc) {
                ConnectionHolder[] h = new ConnectionHolder[mc];
                System.arraycopy(this.inuse, 0, h, 0, this.topInUse);
                this.inuse = h;
            }
            this.notifyAll();
        }
    }

    /**
     * Change the number of spare connections to be held open by this pool. Can
     * be changed at runtime and the pool will adapt to the new setting
     *
     * @param mc
     */
    public synchronized void setMaxSpareConnections(int mc) {
        this.maxSpareConnection = mc;
        if (this.spare == null) {
            this.spare = new ConnectionHolder[mc];
        } else {
            if (this.spare.length < mc) {
                ConnectionHolder[] h = new ConnectionHolder[mc];
                System.arraycopy(this.spare, 0, h, 0, this.topSpare);
                this.spare = h;
            }
        }
    }

    /**
     * Change the number of times a connection from this pool may be used before it will be dumped
     */
    public synchronized void setConnectionMaxUse(int connectionMaxUse) {
		this.connectionMaxUse = connectionMaxUse;
	}
    
    /**
     * Change the number of milliseconds a connection will be kept open at least
     * before it will be considered stale by the pool and closed at the next
     * opportunity.
     *
     * @param mc
     */
    public synchronized void setConnectionExpiration(int ex) {
        this.connectionExpiration = ex;
        if (this.task != null) {
            this.task.cancel();
        }
        this.task = new TimerTask() {
            public void run() {
                synchronized (PoolingDataSource.this) {
                    PoolingDataSource.this._checkSpareConnections();
                }
            }
        };
        if (this.timer != null) {
        	this.timer.cancel();
        }
        this.timer = TimerUtil.createTimer("DataSource Connection Reaper ("+name+")",true);
        this.timer.scheduleAtFixedRate(this.task, this.connectionExpiration, this.connectionExpiration);
    }

    private final static Logger logger = Logger.getLogger(PoolingDataSource.class.getName());    
}

