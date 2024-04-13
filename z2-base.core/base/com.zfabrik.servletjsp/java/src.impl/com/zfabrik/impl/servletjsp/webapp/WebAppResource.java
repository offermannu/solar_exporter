/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.servletjsp.webapp;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.session.DefaultSessionCache;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.MetaInfConfiguration;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.impl.servletjsp.server.IWebServer;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.servletjsp.Constants;
import com.zfabrik.util.expression.X;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.util.threading.ThreadUtil;
import com.zfabrik.work.WorkUnit;
import com.zfabrik.workers.worker.HomeHandle;

/**
 * The WebAppResource abstracts a web app context that may be used over time by a
 * web app. Since Jetty's WebAppContext requires explicit stop and as to avoid
 * memory leaks, we have explicit invalidation handling managing the web app
 * context.
 * 
 * The actual web app component retrieves this resource and registers as its
 * dependency.
 * 
 * A web component may declare dependent web components by (context) paths.
 * 
 * @author hb
 * 
 * 
 */
public class WebAppResource extends Resource implements IDependencyComponent, HttpSessionListener {
	private final static String TYPE = Constants.WEBAPP_TYPE;
	private static final String WEB_CONTENT = "WebContent";
	private final static String RESOURCES = "webapp.folder";

	private String name;

	private WebAppContextWrapper wac;
	private int openSessions;
	private File folder;


	// JMX
	private ObjectName on; 
	
	public static interface WebAppMBean {
		Date getStartTime();
		long getSessions();
		long getMaxSessions();
		int getMaxInactiveInterval();
		String getContextPath();

		long getRequests();
		long getPageImpressions();
	}
	
	private class WebAppMBeanImpl implements WebAppMBean {
		private SessionHandler getsm() {
			return wac.getSessionHandler();
		}
		public String getContextPath() {
			if (wac!=null) {
				return wac.getContextPath();
			} else {
				return null;
			}
		}
		public int getMaxInactiveInterval() {
			if (wac!=null) {
				return getsm().getMaxInactiveInterval();
			} else {
				return -1;
			}
		}
		public long getMaxSessions() {
			if (wac!=null) {
				SessionCache sc = getsm().getSessionCache();
				if (sc instanceof DefaultSessionCache) {
					return ((DefaultSessionCache)sc).getSessionsMax();
				}
			}
			return -1l;
		}
		public long getSessions() {
			if (wac!=null) {
				SessionCache sc = getsm().getSessionCache();
				if (sc instanceof DefaultSessionCache) {
					return ((DefaultSessionCache)sc).getSessionsCurrent();
				}
			}
			return -1l;
		}
		public long getRequests() {return wac.getRequests();}
		public long getPageImpressions() {return wac.getPageImpressions();}

		public Date getStartTime() {
			if (startTime!=0) {
				return new Date(startTime);
			}
			return null;
		}
		
	}
	// - end JMX -
	
	//
	private IWebServer server;
	// keep start time
	private long startTime;
	

	public WebAppResource(String name) {
		this.name = name;
	}

	@Override
	public synchronized <T> T as(Class<T> clz) {
		if (Handler.class.equals(clz)) {
			_load();
			return clz.cast(this.wac);
		}
		if (Object.class.equals(clz)) {
			_load();
			return clz.cast(this);
		}
		if (WebAppResource.class.equals(clz)) {
			return clz.cast(this);
		}
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}

	@Override
	public synchronized void invalidate() throws ResourceBusyException {
		_unload();
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
	 * 
	 * This is used to count sessions and to reflect open sessions in lease counting
	 * in conjunction with worker process detach handling!
	 */
	public synchronized void sessionCreated(HttpSessionEvent arg0) {
		this.openSessions++;
		if (Foundation.isWorker()) {
			HomeHandle.instance().getWorkerLease().increaseLease();
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("New session in WebAppContext (now: " + this.openSessions + "): " + this.name);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
	 * See above
	 */
	public synchronized void sessionDestroyed(HttpSessionEvent arg0) {
		this.openSessions--;
		if (Foundation.isWorker()) {
			HomeHandle.instance().getWorkerLease().decreaseLease();
		}
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("Removed session from WebAppContext (now: " + this.openSessions + "): " + this.name);
		}
	}

	public synchronized void loadForServer(IResourceHandle sh) {
		_loadForServer(sh);
	}

	private void _load() {
		this._loadForServer(null);
	}

	private void _loadForServer(final IResourceHandle sh) {
		if (sh != null && this.wac != null) {
			if (this.server != sh.as(IWebServer.class)) {
				// refuse if loaded by other server.
				// TODO: overcome this stupid limitation
				throw new IllegalStateException("Web component already loaded for other server (" + sh.getResourceInfo().getName() + "): "
						+ this.name);
			}
		}
		if (this.wac == null) {
			ThreadUtil.cleanContextExecute(this.getClass().getClassLoader(),new Callable<Object>() {
				public Object call() throws Exception {
					// -------------------------------
					logger.info("Starting WebApp: " + WebAppResource.this.name);

					IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(WebAppResource.this.name);

					String serverComponent=null;
					IResourceHandle serverHandle = sh;
					if (serverHandle == null) {
						// get the server
						serverComponent = desc.getProperties().getProperty(Constants.SERVER);
						serverHandle = IComponentsLookup.INSTANCE.lookup(serverComponent, IResourceHandle.class);
					} else {
						serverComponent = serverHandle.getResourceInfo().getName();
					}
					IWebServer ws = serverHandle.as(IWebServer.class);
					if (ws == null) {
						throw new IllegalStateException("Failed to retrieve server (" + serverComponent + "): " + WebAppResource.this.name);
					}
					ws.start();
					handle().addDependency(serverHandle);
					WebAppResource.this.server = ws;

					// check required apps and load for the same server
					String required = desc.getProperties().getProperty(Constants.REQUIRED_PATHS);
					if (required != null) {
						StringTokenizer tk = new StringTokenizer(required, ",");
						while (tk.hasMoreTokens()) {
							String path = tk.nextToken().trim();
							Collection<String> res = IComponentsManager.INSTANCE.findComponents(X.var(IComponentDescriptor.COMPONENT_TYPE)
									.eq(X.val(TYPE)).and(X.var(Constants.PATH).eq(X.val(path))));
							if (res.size() > 1) {
								throw new IllegalStateException("Found more than one web app by path " + path + " (" + res
										+ ") when resolving for required paths: " + WebAppResource.this.name);
							}
							if (res.size() == 0) {
								throw new IllegalStateException("Found no web app by path " + path + " when resolving for required paths: " + WebAppResource.this.name);
							}
							String cn = res.iterator().next();
							IResourceHandle rh = IComponentsLookup.INSTANCE.lookup(cn, IResourceHandle.class);
							handle().addDependency(rh);
							// load it
							rh.as(WebAppResource.class).loadForServer(serverHandle);
						}
					}

					// set web content root folder
					File root = IComponentsManager.INSTANCE.retrieve(WebAppResource.this.name);
					WebAppResource.this.folder = new File(desc.getProperties().getProperty(RESOURCES, WEB_CONTENT));
					if (!WebAppResource.this.folder.isAbsolute()) {
						WebAppResource.this.folder = new File(root, WebAppResource.this.folder.getPath());
					}

					// context path
					String path = desc.getProperty(Constants.PATH);
					if (path == null)
						path = "/" + WebAppResource.this.name;
					// jetty needs a leading slash
					if (!path.startsWith("/"))
						path = "/" + path.trim();

					if (logger.isLoggable(Level.FINE)) {
						logger.fine("Creating Web app context for path " + path + " at folder " + WebAppResource.this.folder + ": " + WebAppResource.this.name);
					}

					// create the actual web app context with jetty
					// overwrite doHandle to implement our workhandler wrapping
					WebAppContextWrapper wac = new WebAppContextWrapper();

					try {
						ws.configure(wac);
						wac.setContextPath(path);
						wac.setWar(WebAppResource.this.folder.getAbsolutePath());
						
						// Set this to make sure that all Jars on the classpath are
						// candidates for TLD retrieval.
						// In Jetty 9 this is the way to go as config in jetty-web.xml is read too late to matter
						// for resource identification in WebInfConfiguration
						//
						// we accept a jar, unless it is a -sources.jar
						// this behaviour can be overridden using a config prop
						String jts = desc.getProperties().getProperty(Constants.CONTAINER_INCLUDE_JAR_PATTERN,"(?!.+-sources\\.).*\\.jar$").trim();
						wac.setAttribute(MetaInfConfiguration.CONTAINER_JAR_PATTERN, jts);

						
						// in order to not 
						// a) loose the work folder when Jetty stops (and so break multi-worker access in for example gateway)
						// b) have race conditions during JSP compile,
						// we use a temp dir per worker node gen.
						String workerHex = Integer.toHexString(Foundation.getProperties().getProperty(Foundation.PROCESS_WORKER,"").hashCode());
						// temp directory 
						File r = new File(IComponentsManager.INSTANCE.retrieve(WebAppResource.this.name), "gen/jetty/"+workerHex);
						r.mkdirs();
						wac.setTempDirectory(r);
						
						// set to true to make sure Jetty is not removing the work folder 
						// wac.setPersistTempDirectory(true);
	
						// get the parent class loader
						IResourceHandle jrh = JavaComponentUtil.getJavaComponent(WebAppResource.this.name);
						handle().addDependency(jrh);
						IJavaComponent jc = jrh.as(IJavaComponent.class);
						URLClassLoader parent;
						if (jc != null) {
							parent = jc.getPrivateLoader();
						} else {
							parent = (URLClassLoader) HttpServlet.class.getClassLoader();
						}
						
						// we do explicitly set parent first priority.
						// if not, we do not only have the error prone web app first loading 
						// policy, we also buy into having certain classes filtered (including jetty configuration classes) 
						
						
	//					
	//					/**
	//					/* need to explicitly tell jetty to load from parent first
	//					 * 
	//					 */
						wac.setParentLoaderPriority(true);
	
						//
						// Define the web apps class loader.
						// Note that Jetty will set the context class loader for the web app when 
						// invoking init stuff so that we do not need to do that here.
						// Instead, setting it here would lead to errors as jetty refuses to provide
						// Jetty system classes to web app class loaders (whatever that helps...)
						//
						
						WebAppClassLoader cl = new WebAppClassLoader(parent, wac);
						
							
						wac.setClassLoader(cl);
						wac.setParentLoaderPriority(true);

						// tunnel through the current component name
						wac.setAttribute(IComponentDescriptor.COMPONENT_NAME, WebAppResource.this.name);
						// start the context (i.e. let Jetty initialize it)
						logger.fine("Starting Jetty WebAppContext: " + wac.getContextPath());
						// note that Jetty 8 likes to log a lot...
						// https://jira.codehaus.org/browse/CARGO-1095
						wac.start();
						//
						// Register the web app resource as event listener so we can track 
						// session create and destroy
						// Note in Jetty 8 this MUST happen after context start !!
						//
						wac.addEventListener(WebAppResource.this);
						logger.fine("Web App Context started: " + wac + " (running:" + wac.isRunning() + ", failed:" + wac.isFailed()
								+ ", started" + wac.isStarted() + ")");
						if (wac.isFailed()) {
							logger.warning("Web App Context start failed (not registering): " + WebAppResource.this.name);
							throw new RuntimeException("Web App Context start failed (not registering): " + WebAppResource.this.name);
						}
						;
						if (wac.getUnavailableException() != null) {
							logger.log(Level.WARNING, "Web App Context start failed with error: " + WebAppResource.this.name,
									wac.getUnavailableException());
							throw new RuntimeException("Web App Context start failed with error: " + WebAppResource.this.name,
									wac.getUnavailableException());
						}
						// this adds it to the handler collection in
						// ServerResource
						ws.addHandler(wac);
						WebAppResource.this.wac = wac;
						// no collection for this resource!
						handle().adjust(0, Long.MAX_VALUE, IResourceHandle.STRONG);
						// register some JMX bean
						ObjectName on = ObjectName.getInstance("zfabrik:type=" + WebAppResource.class.getName() + ",name=" + name);
						ManagementFactory.getPlatformMBeanServer().registerMBean(new StandardMBean(new WebAppMBeanImpl(), WebAppMBean.class), on);
						WebAppResource.this.on = on;
						startTime = System.currentTimeMillis();
						logger.info("Done starting Web App (" + wac.getContextPath() + "): " + WebAppResource.this.name);
						return null;
					} finally {
						if (WebAppResource.this.wac==null) {
							// means we were not successful. Unregister
							try {
								logger.warning("Unregistering due to unsuccesful start: "+WebAppResource.this.name);
								ws.removeHandler(wac);
							} catch (Exception e) {
								logger.log(Level.WARNING, "Error during unregistration: "+WebAppResource.this.name,e);
							} finally {
								try {
									wac.stop();
								} catch (Exception e) {
									logger.log(Level.WARNING, "Error during fail stop: "+WebAppResource.this.name,e);
								}
							}
						}
					}
				}
			});
		}
	}

	private void _unload() {
		try {
			if (this.on!=null) {
				try {
					ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
//					logger.info("unregistered "+on);
					this.on = null;
				} catch (Exception e) {
					logger.log(Level.WARNING, "Error during unregistration of WebApp MBean (" + this.on + "): " + this.name);
				}
			}
			
			if (this.wac != null) {
				// be nice to recursion (make sure we do not try to stop twice)
				final WebAppContext wc = this.wac;
				this.wac = null;
				try {
					logger.fine("Stopping web component: " + this.name);
					WorkUnit.work(
						new Callable<Void>() {
							public Void call() throws Exception {
								return ThreadUtil.cleanContextExceptionExecute(
									wc.getClassLoader(), 
									new Callable<Void>() {
										public Void call() throws Exception {
											logger.info("Stopping Web App ("+wc.getContextPath()+"): " + WebAppResource.this.name);
											server.removeHandler(wc);
											wc.stop();
											wc.destroy();
											return null;
										}
									}
								);
							}
						}
					);
				} catch (Exception e) {
					logger.log(Level.WARNING, "Error during context stop: " + this.name, e);
				}
			}
		} finally {
			// this.webAppKeeper = null;
			this.openSessions = 0;
			this.wac = null;
			this.folder = null;
			this.server = null;
			this.startTime=0;
			handle().adjust(0, Long.MAX_VALUE, IResourceHandle.WEAK);
		}
	}

	// dependency component
	public synchronized void prepare() {
		this._load();
	}
		
	private final static Logger logger = Logger.getLogger(WebAppResource.class.getName());

}
