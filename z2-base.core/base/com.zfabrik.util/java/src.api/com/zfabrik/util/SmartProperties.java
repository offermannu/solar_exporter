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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Properties implementation supporting property value substitution. I.e. creating SmartProperties from Properties <code>p</code> defining a property <code>host=localhost</code>,
 * a property definition <code>url = http://${host}/path</code> will be evaluated to <code>url = http://localhost/path</code>.
 * 
 * @author udoo
 *
 */
public class SmartProperties extends Properties {

	private static final long serialVersionUID = -8298083319225740552L;

	private final PropertyExpressionResolver myResolver;
	private final Properties p_in, p_out;
	private boolean isResolved;
	
	public SmartProperties() {
		this(null);
	}
	
	public SmartProperties(Properties defaults) {
		super();
		this.p_in = new Properties(defaults);
		this.p_out = new Properties(defaults);
		this.myResolver = new PropertyExpressionResolver(this.p_in, this.p_out);
		this.isResolved = true;
	}

	@Override
	public synchronized Object get(Object key) {
		resolve();
		return this.p_out.get(key);
	}
	
	@Override
	public String getProperty(String key) {
		resolve();
		return this.p_out.getProperty(key);
	}
	
	@Override
	public String getProperty(String key, String defaultValue) {
		resolve();
		return this.p_out.getProperty(key, defaultValue);
	}
	
	@Override
	public synchronized boolean contains(Object value) {
		resolve();
		return this.p_out.contains(value);
	}
	
	@Override
	public boolean containsValue(Object value) {
		resolve();
		return this.p_out.containsValue(value);
	}
	
	@Override
	public synchronized void clear() {
		resolvedYes();
		this.p_out.clear();
		this.p_in.clear();
	}
	
	@Override
	public synchronized Enumeration<Object> elements() {
		resolve();
		return this.p_out.elements();
	}
	
	@Override
	public Set<java.util.Map.Entry<Object, Object>> entrySet() {
		resolve();
		return this.p_out.entrySet();
	}
	
	@Override
	public synchronized Enumeration<Object> keys() {
		resolve();
		return this.p_out.keys();
	}
	
	@Override
	public Set<Object> keySet() {
		resolve();
		return this.p_out.keySet();
	}
	
	@Override
	public void list(PrintStream out) {
		resolve();
		this.p_out.list(out);
	}
	
	@Override
	public void list(PrintWriter out) {
		resolve();
		this.p_out.list(out);
	}
	
	
	@Override
	public synchronized void load(InputStream inStream) throws IOException {
		resolvedNo();
		this.p_in.load(inStream);
	}
	
//	public synchronized void load(Reader reader) throws IOException {
//		resolvedNo();
//		this.p_in.load(reader);
//	}
	
	@Override
	public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
		resolvedNo();
		this.p_in.loadFromXML(in);
	}
	
	@Override
	public Enumeration<?> propertyNames() {
		resolve();
		return this.p_out.propertyNames();
	}
	
	@Override
	public synchronized Object put(Object key, Object value) {
		resolvedNo();
		return this.p_in.put(key, value);
	}
	
	@Override
	public synchronized void putAll(Map<? extends Object, ? extends Object> t) {
		resolvedNo();
		this.p_in.putAll(t);
	}

	@Override
	public synchronized Object remove(Object key) {
		resolvedNo();
		this.p_in.remove(key);
		return this.remove(key);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public synchronized void save(OutputStream out, String comments) {
		resolve();
		this.p_out.save(out, comments);
	}
	
	@Override
	public synchronized Object setProperty(String key, String value) {
		resolvedNo();
		return this.p_in.setProperty(key, value);
	}
	
	@Override
	public void store(OutputStream out, String comments) throws IOException {
		resolve();
		this.p_out.store(out, comments);
	}
	
//	public void store(Writer writer, String comments) throws IOException {
//		resolve();
//		this.p_out.store(writer, comments);
//	}
	
	@Override
	public synchronized void storeToXML(OutputStream os, String comment) throws IOException {
		resolve();
		this.p_out.storeToXML(os, comment);
	}
	
	@Override
	public synchronized void storeToXML(OutputStream os, String comment, String encoding) throws IOException {
		resolve();
		this.p_out.storeToXML(os, comment, encoding);
	}
	
//	public Set<String> stringPropertyNames() {
//		resolve();
//		return this.p_out.stringPropertyNames();
//	}
	
	@Override
	public Collection<Object> values() {
		resolve();
		return this.p_out.values();
	}
	
	private void resolve() {
		if (! isResolved) {
			resolvedYes();
			this.myResolver.resolveAll();
		}
	}
	
	private void resolvedYes() {
		this.isResolved = true;
	}
	
	private void resolvedNo() {
		this.isResolved = false;
	}
}
