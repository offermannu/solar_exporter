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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Supports performance tracing of JDBC provided objects from 
 * our pool. 
 * <p>
 * Just set the JUL logger "performance.com.zfabrik.impl.db.data" to at least logging level FINE and it
 * will output statement operation performance data.
 * 
 * @author hb
 *
 */
public class JDBCTracer {

	public static Connection treatThis(Connection conn) throws SQLException {
		// if that performance logger is set to fine, we observe the connection at least!
		if (logger.isLoggable(Level.FINE)) {
			return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class[] { Connection.class },new ConnectionInvocationHandler(conn));
		}
		return conn;
	}

	private final static Logger logger = Logger.getLogger("performance." + JDBCTracer.class.getName());

	// Some reflection stuff to trace specific SQL methods
	private static List<Method> STATEMENT_CREATION;
	static {
		try {
			STATEMENT_CREATION = Arrays.asList(new Method[] { Connection.class.getMethod("createStatement", new Class[] {}),
					Connection.class.getMethod("createStatement", new Class[] { int.class, int.class }) });
		} catch (NoSuchMethodException e) {
			logger.log(Level.SEVERE, "static intialization of " + JDBCTracer.class.getName() + " failed", e);
		}
	}

	private static List<Method> STATEMENT_PREPARE;
	static {
		try {
			STATEMENT_PREPARE = Arrays.asList(new Method[] { Connection.class.getMethod("prepareStatement", new Class[] { String.class }),
					Connection.class.getMethod("prepareStatement", new Class[] { String.class, int.class, int.class }) });
		} catch (NoSuchMethodException e) {
			logger.log(Level.SEVERE, "static intialization of " + JDBCTracer.class.getName() + " failed", e);
		}
	}
	
	// anything traceable in here...
	private static class Traceable {
		protected boolean trace = true;
		private long time;
		
		protected void begin() { 
			this.time=System.currentTimeMillis();
		}

		protected void failed(String s) {
			if (trace) {
				long duration = System.currentTimeMillis() - time;
				logger.log(Level.FINE, s + " - FAILED: " + duration + "ms");
			}
		}

		protected void finished(String s) {
			if (trace) {
				long duration = System.currentTimeMillis() - time;
				logger.log(Level.FINE, s + ": " + duration + "ms");
			}
		}
	}

	// invocation handler for connections
	//
	private static class ConnectionInvocationHandler extends Traceable implements InvocationHandler {
		private Connection connection;

		public ConnectionInvocationHandler(Connection connection) throws SQLException {
			this.connection = connection;
		}

		/**
		 * @see java.lang.reflect.InvocationHandler#invoke(Object, Method,
		 *      Object[])
		 */
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			StringBuffer buffer = new StringBuffer(method.getName());
			if ((args != null) && (args.length > 0)) {
				buffer.append(" (");
				for (int i = 0; i < args.length; i++) {
					if (i > 0)
						buffer.append(", ");
					buffer.append(args[i]);
				}
				buffer.append(")");
			}
			boolean success = false;
			begin();
			try {
				Object obj = method.invoke(connection, args);
				if (STATEMENT_CREATION.contains(method)) {
					if (logger.isLoggable(Level.FINE)) {
						obj = Proxy.newProxyInstance(Statement.class.getClassLoader(), new Class[] { Statement.class }, new StatementInvocationHandler((Statement) obj));
					}
				} else if (STATEMENT_PREPARE.contains(method)) {
					if (logger.isLoggable(Level.FINE)) {
						obj = Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class[] { PreparedStatement.class },new StatementInvocationHandler((PreparedStatement) obj));
					}
				}
				finished(buffer.toString());
				success = true;
				return obj;
			} finally {
				if (!success)
					failed(buffer.toString());
			}
		}
	}

	//
	// begin class StatementWrapper
	//
	private static class StatementInvocationHandler extends Traceable implements InvocationHandler {
		private Statement statement;

		public StatementInvocationHandler(Statement statement) {
			this.statement = statement;
		}

		/**
		 * @see java.lang.reflect.InvocationHandler#invoke(Object, Method,
		 *      Object[])
		 */
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// some methods will be traced only if logger on FINER
			boolean log = true;
			if (!logger.isLoggable(Level.FINER)) {
				log = method.getName().startsWith("execute");
			}
			
			StringBuffer buffer = new StringBuffer(method.getName());
			if ((args != null) && (args.length > 0)) {
				buffer.append(" (");
				for (int i = 0; i < args.length; i++) {
					if (i > 0)
						buffer.append(", ");
					buffer.append(args[i]);
				}
				buffer.append(")");
			}
			boolean success = false;
			begin();
			try {
				Object obj = method.invoke(statement, args);
				if (log) {	finished(buffer.toString()); }
				success = true;
				return obj;
			} finally {
				if (!success) {
					if (log) { failed(buffer.toString()); }
				}
			}
		}
	}

}
