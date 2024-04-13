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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.sql.DataSource;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.mchange.v2.c3p0.DataSources;
import com.mchange.v2.c3p0.impl.IdentityTokenResolvable;
import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.PropertyExpressionResolver;
import com.zfabrik.util.threading.ThreadUtil;
import com.zfabrik.work.WorkUnit;

/**
 * Implementation of component type <b>javax.sql.DataSource</b>. This implementation supports a pooled data source implementation that 
 * integrates with Z2's unit of work mechanism (see {@link WorkUnit}).
 * <p>
 * A component of type <b>javax.sql.DataSource</b> can be configured in various ways, controlled by the following 
 * parameters:
 * 
 * <dl>
 * <dt>ds.type</dt><dd>This can be either <b>ZFabrikPoolingDataSource</b> or <b>NativeDataSource</b>. In the first case, the data source will be pooled 
 * using {@link PoolingDataSource}. In that case the pool configuration parameters in {@link PoolingDataSource} should be applied</dd>
 * 
 * <dt>ds.dataSourceClass</dt><dd>If set, the specified data source class will be loaded as data source implementation using the private class loader
 * of the Java module of the component holding the data source definition. When specifying this class in conjunction with the <code>ds.type=ZFabrikPoolingDataSource</code>
 * configuration properties will be applied to both and the pool will request new connections from the specified data source. Alternatively, the pool may be configured
 * to use a driver class. See {@link PoolingDataSource} for more details.</dd>
 * 
 * <dt>ds.dataSourceJNDIName</dt><dd>If <code>ds.dataSourceClass</code> is not set, this JNDI name will be tried to lookup a data source that will
 * serve as connection provider. During lookup the thread context class loader will be set to the private class loader
 * of the Java module of the component holding the data source definition. When specifying this class in conjunction with the <code>ds.type=ZFabrikPoolingDataSource</code>
 * configuration properties will be applied to both and the pool will request new connections from the specified data source. Alternatively, the pool may be configured
 * to use a driver class. See {@link PoolingDataSource} for more details.</dd>
 * 
 * <dt>ds.enlist</dt><dd>This can be either <b>workUnit</b>, <b>jta</b>, or <b>none</b>. 
 * <p>
 * If set to <b>workUnit</b>, connections will be enlisted with the 
 * {@link WorkUnit} and only committed, rolled back, or closed when the unit of work was completed. Transparently this integrates with Z2's 
 * pseudo-distributed JTA implementation. Please check the <a href="http://redmine.z2-environment.net">Wiki</a> at  for more information.
 * If no {@link WorkUnit} is currently active, a non-enlisted connection will be returned.
 * </p> 
 * <p>
 * If set to <b>jta</b> connections will be enlisted with the {@link WorkUnit} as above but removed after commit or rollback. This prevents, that a connection 
 * will be visible for longer than a transaction and it will instead be reinitialized with the pool before it is provided again. 
 * If no JTA transaction is currently active, a non-enlisted connection will be returned. This prevents that any non-transactional code may effect connection
 * state (in particular such as auto commit settings) for transactional connections.
 * </p>
 * <p>
 * If set to <b>none</b> connections will not be enlisted but simply handed out from the data source.
 * </p>
 * <p>
 * It is recommended to use <b>jta</b>.
 * </p> 
 * </dd>
 * <dt>ds.prop.abc</dt><dd>Attempts to set the bean property <b>abc</b> on the data source (and on PoolingDataSource, if used). A setter method <code>setAbc</code>
 * will be used for that. The property <b>ds.propType.abc</b> defines the signature required.</dd>
 * <dt>ds.propType.abc</dt><dd>Defines the type of a configuration property as in <b>ds.prop.abc</b>. Can be any of <b>int</b>,<b>string</b>, or <b>boolean</b>. Defaults to <b>string</b>.</dd>
 * 
 * @author hb
 */
public class DataSourceResource extends Resource {
	final static Logger LOG = Logger.getLogger(DataSourceResource.class.getName());
	
	/**
	 * Component type of the resource
	 */
	public final static String TYPE = "javax.sql.DataSource";
	
	public static enum Enlistment {
		/**
		 * No enlisting. 
		 * A call to getConnection returns a connection from the pool. 
		 */
		none,
		/**
		 * Enlisted in Work Unit. If there is none behaves like {@link #none}. 
		 * Once enlisted, any call to {@link DataSource#getConnection()} returns the same connection.  
		 */
		workUnit, 
		/**
		 * Enlisted in Work Unit, if there is a user transaction 
		 * already bound. If there is none behaves like {@link #none}.
		 *  
		 * Once enlisted, any call to {@link DataSource#getConnection()} returns the same connection
		 * for the duration of the transaction. Once the transaction is committed or rolled back,
		 * the connection is unbound.  
		 */
		jta
	}
	
	private static final Class<?> INT_SIGNATURE = int.class;
	private static final Class<?> STRING_SIGNATURE = String.class;
	private static final Class<?> BOOLEAN_SIGNATURE = boolean.class;

		
	public static final String STRING = "string";
	public static final String INT = "int";
	public static final String BOOLEAN = "boolean";
	public static final String DS_PROP_TYPE_PREFIX = "ds.propType.";
	public static final String DS_PROP_PREFIX = "ds.prop.";

	public static final String DS_TEMPLATE_COMPONENT = "ds.templateComponent";
	public static final String DS_TYPE = "ds.type";
	public static final String DS_CLASS = "ds.dataSourceClass";
	public static final String DS_JNDI = "ds.dataSourceJNDIName";

	public static final String TYPE_COMBO   = "ComboPooledDataSource";
	public static final String TYPE_NATIVE  = "NativeDataSource";
	public static final String TYPE_ZFABRIK = "ZFabrikPoolingDataSource";

	public static final String DS_ENLIST = "ds.enlist";

	
	// helper method for Java 6/7 compatibility on DataSource
	static Logger getParentLogger(Object instance, Method method) throws SQLFeatureNotSupportedException {
		if (method!=null) {
			try {
				return (Logger) method.invoke(instance);
			} catch (InvocationTargetException e) {
				if (e.getCause() instanceof SQLFeatureNotSupportedException) {
					throw (SQLFeatureNotSupportedException) e.getCause();
				}
				throw new RuntimeException(e);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
    	throw new UnsupportedOperationException();
	}

	
	//

	private String name;
	private DataSourceWrapper ds;
	private Enlistment enlistment;
	private Properties origProps;
	private Properties resolvedProps;
	// house keeping!
	private IResourceHandle tplh;

	public DataSourceResource(String name) {
		this.name = name;
	}

	public Enlistment getEnlistment() {
		return enlistment;
	}

	public DataSourceWrapper getDataSource() {
		return ds;
	}
	
	public String getName() {
		return name;
	}
	
	public synchronized <T> T as(Class<T> clz) {
		if (DataSource.class.equals(clz) || Object.class.equals(clz)) {
			_load();
			return clz.cast(this.ds);
		} else 
		if (Properties.class.equals(clz)) {
			_loadProps();
			// return the unresolved properties! So that referring data sources may provide their own overrides
			return clz.cast(this.origProps);
		}
		return null;
	}

	private  void _loadProps() {
		if (this.ds == null) {
			IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(this.name);
			if (desc == null)
				throw new IllegalStateException("Component descriptor not found: " + this.name);

			Properties props = new Properties();

			// check for a template.
			String tplc = desc.getProperties().getProperty(DS_TEMPLATE_COMPONENT);
			if (tplc!=null) {
				// yep... got one. Make ourself dependent and mix the props!
				this.tplh = IComponentsLookup.INSTANCE.lookup(tplc,IResourceHandle.class);
				Properties tplp = this.tplh.as(Properties.class);
				if (tplp==null) {
					throw new IllegalStateException("Template component ("+tplc+") for data source not found: "+this.name);
				}
				handle().addDependency(tplh);
				// start with the template
				props.putAll(tplp);
			}
			// and with higher pref. the local props
			props.putAll(desc.getProperties());
			this.origProps = props;
			this.resolvedProps = new Properties();
			// resolve all expressions
			new PropertyExpressionResolver(this.origProps,this.resolvedProps).resolveAll();
		}
	}
	
	
	protected static ClassLoader getClassLoader(String name) {
    	IJavaComponent jc = JavaComponentUtil.getJavaComponent(name).as(IJavaComponent.class);
    	if (jc==null) {
    		return DataSourceResource.class.getClassLoader();
    	} else {
    		return jc.getPrivateLoader();
    	}
	}
	
	private  void _load() {
		if (this.ds == null) {
			this._loadProps();
		
			String ts = this.resolvedProps.getProperty(DS_TYPE);
			if (ts == null) {
				throw new IllegalStateException("Must specify data source type (" + DS_TYPE + " in {" + TYPE_COMBO + ", " + TYPE_NATIVE + ", "+TYPE_ZFABRIK+"}): "
						+ this.name);
			}
			final String type = ts.trim();

			String enl = this.resolvedProps.getProperty(DS_ENLIST);
			if (enl != null) {
				enl = enl.trim();
				try {
					this.enlistment = Enlistment.valueOf(enl);
				} catch (Exception e) {
					throw new IllegalStateException(
						"Property " + DS_ENLIST + " must be in {" + 
						Arrays.asList(Enlistment.values()).stream().map(Enlistment::name).collect(Collectors.joining(", ")) + 
						"}: " + this.name
					);
				}
			}

			DataSource lds = null;
			try {
				lds = ThreadUtil.cleanContextExceptionExecute(new Callable<DataSource>() {
					public DataSource call() throws Exception {
						// data source to keep and data source to configure as well
						DataSource ds,dc;
						if (TYPE_COMBO.equals(type)) {
							ComboPooledDataSource cds = new ComboPooledDataSource(false);
							cds.setIdentityToken(name);
							IdentityTokenResolvable.doResolve(cds);
							dc=ds=cds;
						} else if (TYPE_NATIVE.equals(type)) {
							DataSource dds = createDataSource();
							if (dds == null) {
								throw new IllegalStateException("Must specify data source (" + DS_CLASS + " or "+ DS_JNDI+") when using type " + TYPE_NATIVE	+ ": " + name);
							}
							ds=dc=dds;
						} else if (TYPE_ZFABRIK.equals(type)) {
							DataSource dds = createDataSource();
							if (dds!=null) {
								dc= dds;
								ds= new PoolingDataSource(handle(), name, dc);
							} else {
								dc=ds= new PoolingDataSource(handle(), name);
							}
							
						} else {
							throw new IllegalStateException("Unsupported data source type (" + DS_TYPE + "=" + type + "): " + name);
						}
						for (Object k : resolvedProps.keySet()) {
							String na = (String) k;
							if (na.startsWith(DS_PROP_PREFIX)) {
								na = na.substring(DS_PROP_PREFIX.length());
								String t = resolvedProps.getProperty(DS_PROP_TYPE_PREFIX + na, STRING);
								if (t != null) {
									t = t.toLowerCase().trim();
								}
								String methodName = "set" + Character.toUpperCase(na.charAt(0)) + na.substring(1);
								String val = resolvedProps.getProperty((String) k);
								if (val != null) {
									val = val.trim();
								} else {
									throw new IllegalArgumentException("Datasource property " + na + " has no value: " + name);
								}
								if (INT.equals(t)) {
									invokeSetter(methodName, INT_SIGNATURE, Integer.parseInt(val), dc,ds);
								} else if (STRING.equals(t)) {
									invokeSetter(methodName, STRING_SIGNATURE, val, dc,ds);
								} else if (BOOLEAN.equals(t)) {
									boolean v = Boolean.parseBoolean(val);
									invokeSetter(methodName, BOOLEAN_SIGNATURE, v, dc,ds);
								} else {
									throw new IllegalArgumentException("unsupported property type " + t);
								}
							}
						}
						return ds;
					}

					// create a data source
					// by class name or by jndi name
					private DataSource createDataSource() {
						return ThreadUtil.cleanContextExecute(
							getClassLoader(name),
							new Callable<DataSource>() {
								@Override
								public DataSource call() throws Exception {
									// try by class name first
									String csclz = resolvedProps.getProperty(DS_CLASS,"").trim();
									if (csclz.length()>0) {
										try {
											return (DataSource) Class.forName(csclz.trim(),false,getClassLoader(name))
													.getDeclaredConstructor().newInstance();
										} catch (Exception e) {
											throw new IllegalStateException("Failed to instantiate data source class "+csclz,e);
										}
									}
									// try by component name next
									String jndi = resolvedProps.getProperty(DS_JNDI, "").trim();
									if (jndi.length()>0) {
										return (DataSource) InitialContext.doLookup(jndi);
									}
									return null;
								}
							}
						);
					}

					// sets on all objects that provide the setter
					private void invokeSetter(String methodName, Class<?> signature, Object value, Object ... targets) 	throws Exception {
						for (Object o : targets) {
							try {
								o.getClass().getMethod(methodName, signature).invoke(o, value);
							} catch (NoSuchMethodException nsme) {
								// that's ok. We try the next.
							}
						}
					}
				});

				// wrap
				this.ds = new DataSourceWrapper(this, lds);
			} catch (Exception e) {
				throw new IllegalStateException("Failed to configure data source: " + this.name, e);
			} finally {
				if (this.ds == null) {
					// something failed
					_destroy(lds);
				}
			}
		}
		
	}

	void _destroy(DataSource ds) {
		if (ds instanceof PoolingDataSource) {
			((PoolingDataSource)ds).destroy();
		} else
		if (ds instanceof ComboPooledDataSource) {
			try {
				((ComboPooledDataSource) ds).close();
			} catch (Exception e) {
				LOG.log(Level.WARNING,"Caught exception when closing pooled data source: "+name,e);
			} finally {
				try {
					DataSources.destroy((ComboPooledDataSource) ds);
				} catch (Exception e) {
					LOG.log(Level.WARNING,"Caught exception when destroying pooled data source: "+name,e);
				}
			}				
		}
	}
	
	public synchronized void invalidate() throws ResourceBusyException {
		if (this.ds != null) {
			try {
				this.ds._kill();
			} finally {
				this.ds = null;
				this.resolvedProps=null;
				this.origProps=null;
				this.tplh = null;
			}
		}
	}

	static Method getMethodIfAvailable(Class<?> clz, String methodName, Class<?> ... argsClz) {
		Method m = null;
		try {
			m = clz.getMethod(methodName,argsClz);
		} catch (NoSuchMethodException nsme) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Method "+methodName + " not found on "+clz);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to retrieve Java method "+methodName+" on "+clz,e);
		}
		return m;
	}
	

}
