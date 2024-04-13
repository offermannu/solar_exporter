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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * Some simple utilities that help constructing SQL queries and update statements
 * from field arrays. Parameter sets and filter expressions may be derived from value Java beans and 
 * filter Java beans respectively.
 * <p>
 * Naming conventions: A field name ALPHA_BETA (regardless of case) tranlates to the Java Bean property alphaBeta.
 * </p>
 * <p>
 * Java Bean property conventions: When reading conditions from beans, a property maxAlpha translates into an including upper bound for the field ALPHA, while
 * a property minBeta translates into an excluding lower bound for the field BETA.
 * 
 *  
 * @author hb
 *
 */
public class QueryBuilder {

	/**
	 * constructs INSERT clause for the given table and fields to be used as a prepared statement string
	 * @param table
	 * @param fields
	 * @return
	 */
	public static String insertClause(String table, String[] fields) {
		StringBuilder b = new StringBuilder(100);
		b.append("INSERT INTO \"").append(table).append("\" (");
		boolean first = true;
		for (String f : fields) {
			if (first) {
				first = false;
			} else {
				b.append(",");
			}
			b.append("\"").append(f).append("\"");
		}
		b.append(") VALUES (");
		for (int i=0; i<fields.length;i++) {
			if (i>0) {
				b.append(",");
			}
			b.append("?");
		}
		b.append(")");
		return b.toString();
	}
	
	/**
	 * INSERTs the data defined by the given bean into the given table using the given connection.
	 * The fields names are derived from the bean properties using the following convention:
	 * a) Field names are lower case; b) an underscore is inserted where the property has a case switch to upper case
	 * 
	 * @param conn   the DB connection
	 * @param table  the name of the table
	 * @param bean   the bean providing the names and the data
	 * @return the result of the {@link PreparedStatement#executeUpdate()}
	 */
	public static int insert(Connection conn, String table, Object bean) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO \"").append(table).append("\" (");
		List<PropertyDescriptor> props = _getProperties(bean);
		for (PropertyDescriptor prop : props) {
			sql.append('"').append(_computeFieldName(prop.getName())).append("\",");
		}

		sql.append("\"created_at\",\"updated_at\") VAlUES (");
		
		for (int idx = 0; idx < props.size(); idx++) {
			sql.append("?,");
		}
		sql.append("?,?)");
				
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			int len = props.size();
			for (int idx = 0; idx < len; idx++) {
				prepStmt.setObject(idx+1, _getBeanProp(bean, props.get(idx)));
			}
			
			// set created_at & updated_at
			Timestamp tNow = new Timestamp(System.currentTimeMillis());
			prepStmt.setTimestamp(len + 1, tNow);
			prepStmt.setTimestamp(len + 2, tNow);
			
			result = prepStmt.executeUpdate();
			
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}
	
	/**
	 * INSERTs the data defined by the given map into the given table using the given connection.
	 * The fields names are derived from the map keys using the following convention:
	 * a) Field names are lower case; b) an underscore is inserted where the property has a case switch to upper case
	 * 
	 * @param conn   the DB connection
	 * @param table  the name of the table
	 * @param data   the data map provides the names and the data
	 * @return the result of the {@link PreparedStatement#executeUpdate()}
	 */
	public static int insert(Connection conn, String table, Map<String, Object> data) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("INSERT INTO \"").append(table).append("\" (");
		Set<Map.Entry<String, Object>> props = data.entrySet();
		for (Map.Entry<String, Object> prop : props) {
			sql.append('"').append(prop.getKey()).append("\",");
		}

		sql.append("\"created_at\",\"updated_at\") VAlUES (");
		
		for (int idx = 0; idx < props.size(); idx++) {
			sql.append("?,");
		}
		sql.append("?,?)");
				
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			int idx = 1;
			for (Map.Entry<String, Object> prop : props) {
				prepStmt.setObject(idx++, prop.getValue());
			}			
			
			// set created_at & updated_at
			Timestamp tNow = new Timestamp(System.currentTimeMillis());
			prepStmt.setTimestamp(idx, tNow);
			prepStmt.setTimestamp(idx + 1, tNow);
			
			result = prepStmt.executeUpdate();
			
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}

	/**
	 * constructs UPDATE clause for the given table and fields to be used as a prepared statement string. In general it is necessary to append a WHERE clause
	 * @param table
	 * @param fields
	 * @return
	 */
	public static String updateClause(String table, String[] fields) {
		StringBuilder b = new StringBuilder(100);
		b.append("UPDATE \"").append(table).append("\" SET ");
		boolean first = true;
		for (String f : fields) {
			if (first) {
				first = false;
			} else {
				b.append(",");
			}
			b.append("\"").append(f).append("\"").append("=?");
		}
		return b.toString();
	}
	
	/**
	 * UPDATEs the data defined by the given bean into the given table using the given connection.
	 * The fields names are derived from the bean properties using the following convention:
	 * a) Field names are lower case; b) an underscore is inserted where the property has a case switch to upper case
	 * 
	 * @param conn   the DB connection
	 * @param table  the name of the table
	 * @param bean   the bean providing the names and the data
	 * @param where  a where clause using '?' as place holder for the data
	 * @param whereParams parameters for the '?'s in the where clause
	 * @return the result of the {@link PreparedStatement#executeUpdate()}
	 */
	public static int update(Connection conn, String table, Object bean, String where, Object... whereParams) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("UPDATE \"").append(table).append("\" SET ");
		List<PropertyDescriptor> props = _getProperties(bean);
		int updatedAtPos = 1;
		for (PropertyDescriptor prop : props) {
			String pName = prop.getName();
			sql.append('"').append(_computeFieldName(pName)).append("\"=?,");
			updatedAtPos++;
		}
		sql.append("\"updated_at\"=?");
		
		if (where != null) {
			sql.append(' ').append(where);
		}
		
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			int len = props.size();
			for (int idx = 0; idx < len; idx++) {
				prepStmt.setObject(idx+1, _getBeanProp(bean, props.get(idx)));
			}
			// set updated_at
			prepStmt.setTimestamp(updatedAtPos, new Timestamp(System.currentTimeMillis()));
			
			if (whereParams != null) {
				for (int idx = 0; idx < whereParams.length; idx++) {
					prepStmt.setObject(updatedAtPos + 1 + idx, whereParams[idx]);
				}
			}
			
			result = prepStmt.executeUpdate();
			
		} catch (Exception e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}
	
	/**
	 * UPDATEs the data defined by the given map into the given table using the given connection.
	 * The fields names are derived from the map keys using the following convention:
	 * a) Field names are lower case; b) an underscore is inserted where the property has a case switch to upper case
	 * 
	 * @param conn   the DB connection
	 * @param table  the name of the table
	 * @param data   the data map provides the names and the data
	 * @param where  a where clause using '?' as place holder for the data
	 * @param whereParams parameters for the '?'s in the where clause
	 * @return the result of the {@link PreparedStatement#executeUpdate()}
	 */
	public static int update(Connection conn, String table, Map<String, Object> data, String where, Object... whereParams) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("UPDATE \"").append(table).append("\" SET ");
		Set<Entry<String, Object>> props = data.entrySet();
		int updatedAtPos = 1;
		for (Map.Entry<String, Object> prop : props) {
			sql.append('"').append(_computeFieldName(prop.getKey())).append("\"=?,");
			updatedAtPos++;
		}
		sql.append("\"updated_at\"=?");
		
		if (where != null) {
			sql.append(' ').append(where);
		}
		
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			int idx = 1;
			for (Map.Entry<String, Object> prop : props) {
				prepStmt.setObject(idx++, prop.getValue());
			}

			// set updated_at
			prepStmt.setTimestamp(updatedAtPos, new Timestamp(System.currentTimeMillis()));
			
			if (whereParams != null) {
				for (int idy=0; idy < whereParams.length; idy++) {
					prepStmt.setObject(updatedAtPos + 1 + idy, whereParams[idy]);
				}
			}
			
			result = prepStmt.executeUpdate();
			
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}
	

	/**
	 * constructs a select clause for the given table and fields
	 */
	public static String selectClause(String table, String[] fields) {
		StringBuilder b = new StringBuilder(100);
		b.append("select ");
		boolean first = true;
		for (String f : fields) {
			if (first) {
				first = false;
			} else {
				b.append(",");
			}
			b.append("\"").append(f).append("\"");
		}
		b.append(" from \"").append(table).append("\"");
		return b.toString();
		
	}
	

	/**
	 * run a query
	 */
	public static List<Map<String, Object>> select(final Connection conn, final String table, final String[] fields, final String where, final Object... whereParams) {
		
		List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();
		
		StringBuilder sql = new StringBuilder(200); 
		sql.append("SELECT ");
		if (fields != null && fields.length > 0) { 
			boolean first = true;
			for (String f : fields) {
				if (! first) { sql.append(','); } else { first = false; }
				sql.append('"').append(f).append("\"");
			}
		} else {
			// no fields defined => use *
			sql.append('*');
		}
		sql.append(" FROM \"").append(table).append("\"");
		
		if (where != null) {
			sql.append(' ').append(where);
		}

		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			
			if (whereParams != null) {
				for (int idx=0; idx < whereParams.length; idx++) {
					prepStmt.setObject(idx + 1, whereParams[idx]);
				}
			}
			
			ResultSet rSet = prepStmt.executeQuery();
			while (rSet.next()) {
				Map<String, Object> row = new LinkedHashMap<String, Object>();
				for (String f : fields) {
					row.put(f, rSet.getObject(f));
				}
				result.add(row);
			}
			
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}

		return result;
	}
	
	/**
	 * DELETEs the rows specified by the given where statement in the given table using the given connection 
	 * 
	 * @param conn   the DB connection
	 * @param table  the name of the table
	 * @param where  a where clause using '?' as place holder for the data
	 * @param whereParams parameters for the '?'s in the where clause
	 * @return the result of the {@link PreparedStatement#executeUpdate()}
	 */
	public static int delete(Connection conn, String table, String where, Object... whereParams) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("DELETE FROM \"").append(table).append('"');

		if (where != null) {
			sql.append(' ').append(where);
		}
		
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			for (int idx = 0; idx < whereParams.length; idx++) {
				prepStmt.setObject(1 + idx , whereParams[idx]);
			}
			
			result = prepStmt.executeUpdate();
			
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}

	/**
	 * Returns the number of rows of the given table
	 * 
	 * @param conn
	 * @param table
	 * @return
	 */
	public static int rowsCount(Connection conn, String table) {
		int result; 
		
		StringBuilder sql = new StringBuilder(200);
		sql.append("SELECT count(*) FROM \"").append(table).append('"');
		
		PreparedStatement prepStmt = null;
		try {
			prepStmt = conn.prepareStatement(sql.toString());
			
			ResultSet rSet = prepStmt.executeQuery();
			if (rSet.next()) {
				result = rSet.getInt(1);
			} else {
				return 0;
			}
		} catch (SQLException e) {
			throw new RuntimeException("Unable to execute SQL stmt " + sql, e);
		} finally {
			if (prepStmt != null) try {
				prepStmt.close();
			} catch (SQLException e) { /* IGNORE: This is stupid: cannot close a statement that caused an exception!!! */ }
		}
		return result;
	}
	/**
	 * Reads Java bean property values from the given POJO for the given fields. The set of fields introspected
	 * is constructed from the passed array as well as some additional fields that will be appended.
	 *  
	 * @param bean
	 * @param fields
	 * @param additionals
	 * @return
	 */
	public static Object[] beanParams(Object bean,String[] fields,String ... additionals) {
		try {
			if (additionals!=null) {
				String[] fs = new String[fields.length+additionals.length];
				System.arraycopy(fields,0,fs,0,fields.length);
				System.arraycopy(additionals,0,fs,fields.length,additionals.length);
				fields = fs;
			}
			Class<?> clz = bean.getClass();
			Object[] res = new Object[fields.length];
			for (int i=0;i<fields.length;i++) {
				String fn = fields[i];
				String gn = _computeGetterName(fn);
				Method g = clz.getMethod(gn);
				res[i]=g.invoke(bean);
			}
			return res;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read params from bean",e);
		}
	}

	/**
	 * compute an array of field names given a bean, following our naming conventions (see above). A list of field names
	 * to omit from the resulting array can be specified as convenience.
	 * 
	 * @param clz
	 * @return
	 */
	
	public static String[] beanFields(Class<?> clz, String ... omitted) {
		List<PropertyDescriptor> l = _getProperties(clz);
		Set<String> oms;
		if (omitted!=null) {
			oms = new HashSet<String>(Arrays.asList(omitted));
		} else {
			oms = Collections.emptySet();
		}
		List<String> fns = new ArrayList<String>(l.size());
		for (PropertyDescriptor pd : l) {
			String fn = _computeFieldName(pd.getName());
			if (!oms.contains(fn)) {
				fns.add(fn);
			}
		}
		return fns.toArray(new String[fns.size()]);
	}
	

	/**
	 * Translate the usual wildcards * and ? into SQL wildcards % and _.
	 */
	public static String mask(String s) {
		return s.replace("%","\\%").replace("_","\\_").replace("*", "%").replace("?","_");		
	}

	
	/**
	 * DB field to getter. We use camel case notation. Every underscore in the field name denotes a new camel case term in the result.
	 * E.g. LASTMODIFIED_BY translates to the property name lastmodifiedBy which translates to the getter name getLastmodifiedBy().
	 * @param fn
	 * @return
	 */
	private static String _computeGetterName(String fn) {
		String pn = _computePropertyName(fn);
		return "get"+Character.toUpperCase(pn.charAt(0))+pn.substring(1);
	}

	/**
	 * DB field to property. We use camel case notation. Every underscore in the field name denotes a new camel case term in the result.
	 * E.g. LASTMODIFIED_BY translates to the property name lastmodifiedBy.
	 * @param fn
	 * @return
	 */
	private static String _computePropertyName(String fn) {
		StringBuilder b = new StringBuilder(fn.length());
		int p,a=0;
		String s;
		do {
			p = fn.indexOf('_',a);
			if (p>=0) {
				s = fn.substring(a,p);
				a = p+1;
			} else {
				s = fn.substring(a);
			}
			if (s.length()>0) {
				if (b.length()==0) {
					b.append(s.toLowerCase());
				} else {
					b.append(Character.toUpperCase(s.charAt(0)));
					b.append(s.substring(1).toLowerCase());
				}
			}
		} while (p>=0);
		return b.toString();
	}
	
	private static String _computeFieldName(String prop) {
		StringBuilder result = new StringBuilder(prop.length() * 2);
		int len = prop.length();
		for (int idx = 0; idx < len; idx++) {
			char c = prop.charAt(idx);
			
			if (Character.isUpperCase(c)) {
				if (idx != 0) result.append('_'); // avoid leading _
				c = Character.toLowerCase(c);
			}
			
			result.append(c);
		}
		return result.toString();
	}
	
	
	private static List<PropertyDescriptor> _getProperties(Object bean) {
		if (bean==null) {
			return Collections.emptyList();
		}
		return _getProperties(bean.getClass());
	}
	
	
	private static List<PropertyDescriptor> _getProperties(Class<?> clz) {
		List<PropertyDescriptor> result = new ArrayList<PropertyDescriptor>();
		if (clz != null) {
			try {
				BeanInfo beanInfo = Introspector.getBeanInfo(clz);
				PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
				for (PropertyDescriptor p : props) {
					if (! "class".equals(p.getName()) && p.getReadMethod() != null) {
						result.add(p);
					}
				}
			} catch (IntrospectionException e) {
				throw new RuntimeException("Unable to retrieve properties from bean class " + clz, e);
			}
		}
		return result;
	}
	
	private static Object _getBeanProp(Object bean, PropertyDescriptor prop) {
		Object result;
		try {
			Method getter = prop.getReadMethod();
			getter.setAccessible(true);
			result = getter.invoke(bean);
		} catch (Exception e) {
			throw new RuntimeException("Unable to get Property '" + prop.getName() + "' from bean ", e);
		}
		return result;
	}
	
}
