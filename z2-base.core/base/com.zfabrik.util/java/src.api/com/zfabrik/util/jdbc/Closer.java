/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class Closer {

	public static void close(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception ignored) {
			}
		}
	}

	public static void close(Statement statement) {
		if (statement != null) {
			try {
				statement.close();
			} catch (Exception ignored) {
			}
		}
	}

	public static void close(ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (Exception ignored) {
			}
		}
	}
	
	public static void close(Closeable obj) {
		if (obj != null) {
			try {
				obj.close();
			} catch (Exception ignored) {
			}
		}
	}

	public static void close(Object... objs) {
		for (Object o : objs) {
			try {
				if (o instanceof ResultSet) {
					((ResultSet) o).close();
				} else if (o instanceof Statement) {
					((Statement) o).close();
				} else if (o instanceof Connection) {
					((Connection) o).close();
				} else if (o instanceof Closeable) {
					((Closeable) o).close();
				}
			} catch (Exception ignore) {}
		}
	}

}
