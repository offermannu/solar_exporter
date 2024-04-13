/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util;

import java.util.Properties;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The <code>PropertyExpressionResolver</code> class provides the functionality to resolve expressions in properties.
 * An expression is any character sequence enclosed with ${ and }; all characters including space, umlauts, punctuation chars but except '}'
 * are allowed. 
 * Expressions are resolved by replacing all ${key} sequences by the result of calling getProperty(key) on the input-properties object.
 * One can escape the an expression by $${ which will be replaced by ${.
 * The <code>PropertyExpressionResolver</code> is created using either one <code>Properties</code> object or two. 
 * In the first case the method {@link #resolveAll()} resolves all property-expressions and stores the result in the same object and replaces the original expressions.
 * In the second case the first <code>Properties</code> object hosts the expressions and is unchanged while the results are stored in the second <code>Properties</code> object.
 * 
 */
public class PropertyExpressionResolver {
	
	private final static Pattern EXPR = Pattern.compile("(\\${1,2})\\{([^}]*)(\\})?"); 
	private final Properties p_in;
	private final Properties p_out;

	/**
	 * Constructs a new <code>PropertyExpressionResolver</code> object for the given <code>Properties p_in</code>. 
	 * Calling {@link #resolveAll()} will store all resolved property expressions in <code>p_out</code>.
	 * 
	 * @param p_in input-properties: <code>Properties</code> object containing the expressions
	 * @param p_out output-properties: <code>Properties</code> object which will contain the resolved expressions
	 */
	public PropertyExpressionResolver(Properties p_in, Properties p_out) {
		this.p_in = p_in;
		this.p_out = p_out;
	}
	
	/**
	 * Constructs a new <code>PropertyExpressionResolver</code> object for the given <code>Properties</code>. 
	 * Calling {@link #resolveAll()} will replace all properties in <code>p_in_out</code> by the resolved versions.
	 *
	 * @param p_in_out <code>Properties</code> object containing the expressions which will be replaced by the resolved versions
	 */
	public PropertyExpressionResolver(Properties p_in_out) {
		this(p_in_out, p_in_out);
	}

	/**
	 * Resolves all property-expressions provided by the input-properties object and stores the result into the output-properties.
	 * @return the same <code>Properties</code> object as provided as output-properties to the constructor.
	 */
	public Properties resolveAll() {
		for (Entry<Object, Object> e : this.p_in.entrySet()) {
			Object key = e.getKey();
			Object val = e.getValue();
			
			if (key instanceof String && val instanceof String) {
				String resolved = resolveExpr((String) val);
				this.p_out.setProperty((String) key, resolved);
			} else {
				this.p_out.put(key, val);
			}
		}
		return this.p_out;
	}
	
	/**
	 * Resolves the given expression string using the input-properties object
	 * @param p_expr string which may contain expressions
	 * @return string where all expressions are resolved
	 */
	public String resolveExpr(String p_expr) {
		try {
			return resolveExprInternal(p_expr, 0);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Maximum recursion depth exceeded. Maybe you have circular references in property expression '" + p_expr + "'.");
		}
	}	
	
	/**
	 * Looks up the given key in the input-properties object and resolves the value using the input-properties object
	 * @param key property key in the input-properties object 
	 * @return string where all expressions are resolved
	 */
	public String resolveKey(String key) {
		try {
			return resolveExpr(this.p_in.getProperty(key));
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Maximum recursion depth exceeded. Maybe you have circular references in property '" + key + "'.");		}
	}
	
	private String resolveKeyInternal(String key, int depth) {
		return resolveExprInternal(this.p_in.getProperty(key), depth);
	}

	private String resolveExprInternal(String p_expr, int depth) {

		boolean hasExpression = false;
		
		if (p_expr == null) return null;
		if (depth > 100) throw new IllegalStateException();
		
		StringBuilder result = new StringBuilder(p_expr.length() * 2);
		
		Matcher m = EXPR.matcher(p_expr);
		int p0 = 0, p1 = 0;
		while (m.find()) {
			p1 = m.start();
			result.append(p_expr.substring(p0, p1));
			
			if (m.group(1).length() == 2) {
				// handle escape $${...}
				result.append(m.group().substring(1));
			} else {
				if (m.group(3) == null) throw new RuntimeException("missing '}' in property expression '" + p_expr + "'.");
				
				String var = m.group(2);
				String val = resolveKeyInternal(var, depth+1);
				
				if (val != null) result.append(val);

			}
			p0 = m.end();
			hasExpression = true;

		}

		if (hasExpression) {
			result.append(p_expr.substring(p0));
			return result.toString();
		} else {
			return p_expr;
		}
	}
	
}
