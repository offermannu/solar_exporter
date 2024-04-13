/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.servletjsp.server;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.xml.XmlConfiguration;
import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.naming.jndi.INaming;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.threading.ThreadUtil;

public class ServerResource extends Resource implements IWebServer {
  private final static Logger logger = Logger.getLogger(ServerResource.class.getName());
	
	/*
	 * The jetty server class has a static ShutDownHook member. We want to make 
	 * sure that its not registered (we have our own and we want to be able
	 * to unload that class. That's why we set that system prop here.
	 * 
	 * Also, make sure not to have "Server" on the signatures so that the
	 * class gets initialized in a clean context block (as below), as to make
	 * sure the ShutDownHook thread's acc und context class loader is not
	 * holding on to too much stuff (although the fact that it doesn't get
	 * registered should already fix that leak).
	 */
	
	static {
		System.setProperty("JETTY_NO_SHUTDOWN_HOOK", Boolean.TRUE.toString());
	}
	
	private final static String JETTYXML = "jetty.config"; // !!overwritable
	private final static String DEFAULT_WEBXML = "jetty.default-web.xml"; // !!overwritable
	private final static String OVERRIDE_WEBXML = "jetty.override-web.xml"; // !!overwritable
	
	// Jetty server configuration prefix
	private final static String JETTY_CONF_PREFIX = "jetty.property.";  
	
	private Server server;
	private IComponentDescriptor desc;
	private String name;
	private HandlerCollection myCollection;
	private ObjectName on;
	private File dwebxml;
	private List<File> owebxml;

	public ServerResource(String name) {
		this.name = name;
	}
	
	// -----JMX--------
	public static interface ServerResourceMBean {
	    int getRequests();
	    int getRequestsActive();
	    int getRequestsActiveMax();
	    int getResponses1xx();
	    int getResponses2xx();
	    int getResponses3xx();
	    int getResponses4xx();
	    int getResponses5xx();
	    long getStatsOnMs();
	    long getRequestTimeMax();
	    long getRequestTimeTotal();
	    long getRequestTimeAve();
	}
	
	private class ServerResourceMBeanImpl implements ServerResourceMBean {
		private StatisticsHandler sh;
		public ServerResourceMBeanImpl(StatisticsHandler h) {
			this.sh = h;
		}
		public int getRequests() {return this.sh.getRequests();}
		public int getRequestsActive() {return this.sh.getRequestsActive();}
		public int getRequestsActiveMax() { return this.sh.getRequestsActiveMax();}
		public long getRequestTimeMax() {return this.sh.getRequestTimeMax();}
		public long getRequestTimeTotal() {return this.sh.getRequestTimeTotal();}
		public long getRequestTimeAve() {return (this.getRequests()==0?-1:Math.round( ((double) this.getRequestTimeTotal())/((double)this.getRequests())));}
		public int getResponses1xx() {return this.sh.getResponses1xx();}
		public int getResponses2xx() {return this.sh.getResponses2xx();}
		public int getResponses3xx() {return this.sh.getResponses3xx();}
		public int getResponses4xx() {return this.sh.getResponses4xx();}
		public int getResponses5xx() {return this.sh.getResponses5xx();}
		public long getStatsOnMs() {return this.sh.getStatsOnMs();}
	}
	

	// 

	public void start() {
		synchronized (this) {
			if (server == null) {
				try {
					this.server=ThreadUtil.cleanContextExceptionExecute(()->{
					    logger.info("Starting Web server: "+ServerResource.this.name);
					    // try starting a server
						this.desc = IComponentsManager.INSTANCE.getComponent(name);
						// read descriptors
						this.dwebxml = _getFile(DEFAULT_WEBXML);
						this.owebxml = _getFiles(OVERRIDE_WEBXML);
						
						List<File> jxmlf = _getFiles(JETTYXML);
						Server s = null;
						if (!jxmlf.isEmpty()) {
							// a jetty xml config has been provided, we will use
							// that with pref.
							try {
								//
								// server configuration happens with the class loader
								// of the component that holds the config so that other classes
								// referred to in the config have a chance of being found
								//
								IResourceHandle rh = JavaComponentUtil.getJavaComponent(name); 
								IJavaComponent jc = rh.as(IJavaComponent.class);
								ClassLoader cl = (jc!=null?jc.getPrivateLoader():null);
								ClassLoader ol = Thread.currentThread().getContextClassLoader(); 
								Thread.currentThread().setContextClassLoader(cl);
								try {
								    // Note: In general, we are mimicking
								    // the Jetty way of configuration for
								    // the server.
									  
								    // Configuration properties
								    Properties props = new Properties();
								    // include system props
								    props.putAll(System.getProperties());
								    // include the component level props
								    desc.getProperties().forEach((n,value)->{
								      if (n.toString().startsWith(JETTY_CONF_PREFIX)) {
								        String key = n.toString().substring(JETTY_CONF_PREFIX.length()).trim();
								        logger.log(Level.FINE,()->"Setting jetty configuration property: "+key+"="+value);
                                           props.put(key,value);
								      }
								    });
								    
									// run the chain of configs
									XmlConfiguration prev = null;
									for (File f : jxmlf) {
										logger.fine("Applying Jetty config "+f.getName());
										XmlConfiguration xfg = new XmlConfiguration(new PathResource(f));
										if (prev!=null) {
											// move latest state
											xfg.getIdMap().putAll(prev.getIdMap());
										}
										// set props
										props.forEach((n,v)->xfg.getProperties().put(n.toString(),String.valueOf(v)));
										// run it
										Object o = xfg.configure();
										// check for a server
										if (o instanceof Server) {
											if (s==null) {
												s = (Server) o;
											} else
											if (!s.equals(o)) {
												throw new IllegalStateException("Jetty config for more than one server! Make sure to configure only one (check ids)");
											}
										}
										prev = xfg; 
									}
									
									if (s==null) {
										throw new IllegalStateException("Jetty config did not configure an actual server!");
									}
									
								} finally {
									Thread.currentThread().setContextClassLoader(ol);
								}
								// end server configuration
								
								// check for mbean support
								Handler h = s.getHandler();
								if (h instanceof StatisticsHandler) {
									ObjectName on = ObjectName.getInstance("zfabrik:type=" + ServerResource.class.getName() + ",name=" + name);
									ManagementFactory.getPlatformMBeanServer().registerMBean(new StandardMBean(new ServerResourceMBeanImpl((StatisticsHandler) h),ServerResourceMBean.class), on);
									ServerResource.this.on = on;
								}
							} catch (Exception e) {
								throw new RuntimeException("Failed to apply jetty configuration (" + jxmlf + ") to server: " + name, e);
							}
						} else {
							throw new IllegalStateException("No jetty configuration found at "+jxmlf);
						}
						
						// search for the context handler collection to integrate with (there should be at least one!)
						this.myCollection = _findContextHandlerCollection(s.getHandler()); 
						if (this.myCollection==null) {
							throw new IllegalStateException("No ContextHandlerCollection found in Jetty's handler chain. Check your jetty.xml and make sure there is one (and only one preferrably)");
						}
						
						// web apps need naming! Make the server dependent on the naming initializer
						handle().addDependency(
							IComponentsLookup.INSTANCE.lookup(
								INaming.FEATURE_COMPONENT, 
								IResourceHandle.class
							)
						);
						// Make the server dependent on its java component (doesn't happen automatically by the component wrapper)
						handle().addDependency(JavaComponentUtil.getJavaComponent(name));
						s.start();
						return s;
					});
				} catch (Exception e) {
      				try {
      					// try unregister mbean in case we registered already
      					if (this.on!=null) {
      						ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);							
      					}
      				} catch (Exception f) {
      					logger.log(Level.WARNING,"Error during unregistration of mbean due to previous error (this is really stupid!)",f);
      				} finally {
      					this.on = null;
      				}
      				throw new RuntimeException("Failed to start web server: " + this.name, e);
				}
			}
		}
	}

	private ContextHandlerCollection _findContextHandlerCollection(Handler h) {
		if (h instanceof ContextHandlerCollection) {
			return (ContextHandlerCollection) h;
		} else 
		if (h instanceof HandlerCollection) {
			for (Handler g : ((HandlerCollection) h).getChildHandlers()) {
				ContextHandlerCollection hc = _findContextHandlerCollection(g);
				if (hc!=null) {
					return hc;
				}
			}
		} else
		if (h instanceof HandlerWrapper) {
			return _findContextHandlerCollection(((HandlerWrapper)h).getHandler());
		}
		return null;
	}
	
	private List<File> _getFiles(String propname) {
		List<File> res = new LinkedList<File>();
		String paths = this.desc.getProperty(propname);
		if (paths!=null) {
			StringTokenizer tk = new StringTokenizer(paths,",");
			while (tk.hasMoreTokens()) {
				String path = tk.nextToken().trim();
				File f = new File(path);
				if (!f.isAbsolute()) {
					File folder;
					try {
						folder = IComponentsManager.INSTANCE.retrieve(this.name);
					} catch (Exception e) {
						throw new RuntimeException("Failed to retrieve component folder: " + this.name);
					}
					if (folder == null) {
						throw new IllegalArgumentException("A configuration ("+propname+") for a relative file path ("+path+") has been specified but the server has no component folder: " + this.name);
					}
					f=new File(folder,path);
				}
				if (!f.exists()) {
					throw new IllegalArgumentException("A configuration file ("+f+") has been specified ("+propname+") but was not found at the specified location: " + this.name);
				}
				res.add(f);
			}
		}
		return res;
	}
	
	private File _getFile(String propname) {
		List<File> fs = _getFiles(propname);
		if (fs.isEmpty()) {
			return null;
		}
		if (fs.size()>1) {
			throw new IllegalArgumentException("More than one configuration file has been specified ("+propname+") where at most one is supported: " + this.name);
		}
		return fs.get(0);
	}

	public synchronized void stop() {
		try {
			if (this.server != null) {
				try {
					if (this.on != null) {
						try {
							ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
							this.on = null;
						} catch (Exception e) {
							logger.log(Level.WARNING, "Error during unregistration of Web Server MBean (" + this.on + "): " + this.name);
						}
					}
					this.server.getHandler().stop();
					this.server.getHandler().destroy();
					this.server.stop();
					this.server.destroy();
				} catch (Exception e) {
					logger.warning("error while shutting down server");
				}
			}
		} finally {
			this.server = null;
			this.desc = null;
		}
	}

	public void configure(WebAppContext wac) {
		if (this.dwebxml!=null) {
			wac.setDefaultsDescriptor(this.dwebxml.getAbsolutePath());
		}
		if (this.owebxml!=null) {
			List<String> paths = new LinkedList<String>();
			for (File f : this.owebxml) {
				paths.add(f.getAbsolutePath());
			}
			wac.setOverrideDescriptors(paths);
		}
		wac.setServer(this.server);
	}
	
	public synchronized void addHandler(Handler h) {
		this.myCollection.addHandler(h);
	}

	public synchronized void removeHandler(Handler h) {
		this.myCollection.removeHandler(h);
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		stop();
	}

	public <T> T as(Class<T> clz) {
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(new IDependencyComponent() {
				public void prepare() {
					start();
				}
			});
		} else if (IWebServer.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}

}
