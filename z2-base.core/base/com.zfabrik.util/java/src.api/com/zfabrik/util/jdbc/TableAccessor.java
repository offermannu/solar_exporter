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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class TableAccessor {

	private static final Logger logger = Logger.getLogger(TableAccessor.class.getName());
	
	private final DataSource ds;
	private final String tableName;
	private Connection connection;

	public TableAccessor(DataSource ds, String tableName) {
		this.ds = ds;
		this.connection = null;
		this.tableName = tableName;
	}

	public TableAccessor(Connection conn, String tableName) {
		this.ds = null;
		this.connection = conn;
		this.tableName = tableName;
	}

	private Connection _getConnection() {
		try {
			if (this.connection != null) {
				return this.connection;
			} else {
				return this.ds.getConnection();
			}
		} catch (SQLException e) {
			throw new RuntimeException("Cannot retrieve connection from data source", e);
		}
	}

	public int insert(Object bean) {
		return QueryBuilder.insert(_getConnection(), this.tableName, bean);
	}
	
	public int insert(Map<String, Object> data) {
		return QueryBuilder.insert(_getConnection(), this.tableName, data);
	}

	public int update(Object bean, String where, Object... whereParams) {
		return QueryBuilder.update(_getConnection(), this.tableName, bean, where, whereParams);
	}
	
	public int update(Map<String, Object> data, String where, Object... whereParams) {
		return QueryBuilder.update(_getConnection(), this.tableName, data, where, whereParams);
	}

	public int delete(String where, Object... whereParams) {
		return QueryBuilder.delete(_getConnection(), this.tableName, where, whereParams);
	}

	public List<Map<String, Object>> select(String[] fields, String where, Object... whereParams) {
		return QueryBuilder.select(_getConnection(), this.tableName, fields, where, whereParams);
	}

	public List<Map<String, Object>> select(String[] fields) {
		return QueryBuilder.select(_getConnection(), this.tableName, fields, null);
	}
	
	public int rowsCount() {
		return QueryBuilder.rowsCount(_getConnection(), this.tableName);
	}

	public void resetConnection() {
		this.connection = null;
	}
	
	public String dumpTable() {
		StringBuffer result = new StringBuffer(1024);
		PreparedStatement prepStmt = null;
		try {
			// run SELECT * from "<table>"
			String sql = "SELECT * FROM \"" + this.tableName + "\"";
			prepStmt = _getConnection().prepareStatement(sql);
			ResultSet rSet = prepStmt.executeQuery();
			
			// the result set meta data contains the column names
			ResultSetMetaData metaData = rSet.getMetaData();
			int colCount = metaData.getColumnCount();
			
			for (int idx = 1; idx <= colCount; idx++) {
				if (idx > 1) {
					result.append(" |");
				}
				result.append(' ').append(_ten(metaData.getColumnLabel(idx)));
			}
			result.append('\n');
			for (int idx = 1; idx <= 13 * colCount; idx++) {
				result.append('-');
			}
			result.append('\n');
			
			while(rSet.next()) {
				for (int idx = 1; idx <= colCount; idx++) {
					if (idx > 1) {
						result.append(" |");
					}
					result.append(' ').append(_ten(rSet.getObject(idx)));
				}
				result.append('\n');
			}
			
			return result.toString();
		} catch (Exception e) {
			throw new RuntimeException("Failed to dump table " + this.tableName, e);
		} finally {
			if (prepStmt != null) {
				try {
					prepStmt.close();
				} catch (SQLException e) {
					logger.log(Level.WARNING, "Cannot close prepared statememt", e);
				}
			}
		}
		
	}
	
	private String _ten(Object o) {
		String s = String.valueOf(o) + "          ";
		return s.substring(0, 10);
	}
}
