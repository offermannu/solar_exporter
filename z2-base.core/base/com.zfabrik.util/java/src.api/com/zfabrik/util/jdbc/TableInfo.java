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
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

public class TableInfo {

	public static List<String> getColumns(String tableName, DataSource ds) {
		try {
			return getColumns(tableName, ds.getConnection());
		} catch (SQLException e) {
			throw new RuntimeException("Unable to retrieve connection from data source", e);
		}
	}
	
	public static List<String> getColumns(String tableName, Connection conn) {

		List<String> result = new ArrayList<String>();
		
		ResultSet rSet = null;
		try {
			DatabaseMetaData metaData = conn.getMetaData();
			rSet = metaData.getColumns(null, null, tableName, "*");
			while(rSet.next()) {
				result.add(rSet.getString("COLUMN_NAME"));
			}
		} catch (SQLException e) {
			throw new RuntimeException("Unable to retrieve columns from table " + tableName, e);
		} finally {
			if (rSet != null) {
				try {
					rSet.close();
				} catch (SQLException e) { /* ignore */ }
			}
		}
		
		return result;
	}
}
