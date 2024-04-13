/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway.home;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Socket;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.work.IThreadPool;
import com.zfabrik.work.WorkManager;
import com.zfabrik.workers.home.IWorkerHorde;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * This handler enables the dispatching to a worker process.
 * 
 * It converts a http request into a byte[], sends it to the worker node and writes the response
 * from the return byte[].
 * 
 * This runs on the dispatching process (the home process).
 * 
 * The gateway Jetty configuration for the home process should (essentially) only use this handler to make sure
 * all requests get dispatched properly.
 */
public class GatewayHandlerImpl extends AbstractHandler {
	private final static Logger LOG = Logger.getLogger(GatewayHandlerImpl.class.getName());

	/**
	 * Gateway port system property name. The port that is dispatched to. Or, from another perpective, the port that
	 * the Gateway Server instance on the worker node uses.
	 */
	private static final String COM_ZFABRIK_GATEWAY_PORT = "com.zfabrik.gateway.port";

	
	// JMX

	private ObjectName on;
	
	public interface IGatewayHandlerMBean {
		String getTargetWorkerProcessName();
	}
	
	private class GatewayHandlerMBean implements IGatewayHandlerMBean {
		@Override
		public String getTargetWorkerProcessName() {
			IWorkerProcess wp = findWorkerProcess(null);
			if (wp!=null) {
				return wp.getName();
			}
			return null;
		}		
	}
	
	public static ObjectName getMBeanObjectName() throws MalformedObjectNameException {
		ObjectName on = ObjectName.getInstance("zfabrik:type=" + GatewayHandlerImpl.class.getName());
		return on;
	}
	
	// and the rest
	private GatewaySessionManager sessionManager = new GatewaySessionManager();
	private String targetWorkerProcessComponent;
	private IThreadPool tp;
	private Integer maxReadThreads;
	private int port;
	private int openMessages = 0;
	
	public GatewayHandlerImpl() {
		if (Foundation.isWorker()) {
			throw new IllegalStateException("The Gateway Handler cannot be run on a worker process currently");
		}
	}
	
	@Override
	protected void doStart() throws Exception {
		super.doStart();
		this.tp = WorkManager.get().createThreadPool(GatewayHandlerImpl.class.getName());
		if (this.maxReadThreads==null) {
			// fish from jetty
			ThreadPool t = this.getServer().getThreadPool();
			if (t instanceof QueuedThreadPool) {
				this.maxReadThreads = ((QueuedThreadPool)t).getMaxThreads();
				LOG.fine("Setting max read threads "+this.maxReadThreads+" from server threadpool config");
			}
		} 
		if (this.maxReadThreads==null) {
			throw new IllegalArgumentException("Max read threads not configured (e.g. <Set name=\"maxReadThreads\">20</Set>) and no Jetty Threadpool recognized");
		}
		this.tp.setMaxConcurrency(this.maxReadThreads);
		this.port = getGatewayBasePort(this.targetWorkerProcessComponent);
		// bind an mbean
		try {
			ObjectName on = getMBeanObjectName();
			ManagementFactory.getPlatformMBeanServer().registerMBean(
				new StandardMBean(new GatewayHandlerMBean(),IGatewayHandlerMBean.class), 
				on
			);
			this.on = on;
		} catch (Exception e) {
			LOG.log(Level.SEVERE,"Failed to bind Gateway Handler MBean",e);
		}

	}

	public static int getGatewayBasePort(String workerProcessComponentName) {
		IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(workerProcessComponentName);
		if (desc==null) {
			throw new IllegalStateException("Gateway target worker component ("+workerProcessComponentName+") not found");
		}
		String sp = desc.getProperty(COM_ZFABRIK_GATEWAY_PORT);
		if (sp==null) {
			throw new IllegalStateException("Gateway target worker component ("+workerProcessComponentName+") does not specify "+COM_ZFABRIK_GATEWAY_PORT);
		}
		return Integer.parseInt(sp.trim());
	}
	
	@Override
	protected void doStop() throws Exception {
		try {
			super.doStop();
		} finally {
			try {
				WorkManager.get().releaseThreadPool(GatewayHandlerImpl.class.getName());
			} finally {
				// release mbean
				if (this.on!=null) {
					try {
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
					} catch (Exception e) {
						LOG.log(Level.SEVERE,"Failed to unbind Gateway Handler MBean",e);
					} finally {
						this.on = null;
					}
				}
			}
		}
	}
	
	public void setWorkerProcessComponentName(String workerProcessComponentName) {
		this.targetWorkerProcessComponent = workerProcessComponentName;
	}
	
	public void setMaxReadThreads(int maxReadThreads) {
		this.maxReadThreads = maxReadThreads;
	}


	/**
	 * A simple gate to make sure we wait for request completion
	 */
	private static class Lock {
		private boolean opened;
		
		public synchronized void pass() {
			while (!this.opened) {
				try {
					this.wait();
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		public synchronized void open() {
			this.opened = true;
			this.notifyAll();
		}

	}


	
	@Override
	public void handle(String target, final Request baseRequest, final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
		Response baseResponse = (Response)response;

		// find the worker for the session (if any)
		String sessionId = getSessionId(request);
		IWorkerProcess wp = findWorkerProcess(sessionId);
		if (wp==null) {
			throw new ServletException("Target worker not found");
		}
		synchronized (this) {
			this.openMessages++;
		}
		try {
			long start = System.currentTimeMillis();
			final RequestMarshaller req = new RequestMarshaller(request);
			final ResponseWriter writer = new ResponseWriter(baseRequest,baseResponse);

			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Starting request "+reqToString(request));
			}

			final String name = wp.getName();

			final Lock gate = new Lock();
			
			int port = this.port+Integer.parseInt(name.substring(name.lastIndexOf('@')+1));
			LOG.fine("Dispatching request "+reqToString(request)+" to port "+port);
			final Socket socket = new Socket("localhost", port);

			try {
				// read output
				this.tp.execute(false, new Runnable() {
					public void run() {
						try {
							if (LOG.isLoggable(Level.FINER)) {
								LOG.finer("Start output handling on request "+reqToString(request));
							}
							byte[] buffer = new byte[16384];
							int l;
							while (!writer.isCompleted() && (l=socket.getInputStream().read(buffer))>=0) {
								writer.write(buffer,0,l);
								if (LOG.isLoggable(Level.FINEST)) {
									LOG.finest("Wrote "+l+" bytes of output ("+writer.getBodyBytesSent()+" body so far) to "+reqToString(request));
								}
							}
						} catch (Exception e) {
							LOG.log(Level.WARNING,"Error during response write",e);
						} finally {
							if (LOG.isLoggable(Level.FINER)) {
								LOG.finer("Done output on request "+reqToString(request));
							}
							gate.open();
						}
					}

				});
				

				byte[] c;
				try {
					while ((c=req.getChunk())!=null) {
						socket.getOutputStream().write(c);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (LOG.isLoggable(Level.FINER)) {
					LOG.finer("Done input on request "+reqToString(request));
				}

				gate.pass();
				
			} finally {
				socket.close();
			}
			
			// update lease in session manager
			this.sessionManager.updateSession(writer.getSessionId(),writer.getSessionExpiration(),wp);
			
			if (LOG.isLoggable(Level.FINE)) {
				synchronized (this) {
					LOG.fine(
							"Gateway completed request "+reqToString(request)+" after  "+(System.currentTimeMillis()-start)+"ms. "+
					        (writer.getSessionExpiration()!=null? "Lease expiration now at "+new Date(writer.getSessionExpiration()) +" ("+writer.getSessionExpiration()+")" : "")+
					        ". After this having "+(this.openMessages-1)+" messages in processing and "+this.sessionManager.getSize()+" sessions"
					);
				}
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE,"Request dispatch failed",e);
			baseResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request dispatch failed");
		} finally {
			// mark handled
			baseRequest.setHandled(true);
			synchronized (this) {
				this.openMessages--;
			}
		}
	}

	private String reqToString(HttpServletRequest request) {
		String sid = getSessionId(request);
		return request.getRequestURI()+"@"+request.hashCode()+(sid!=null? "/"+sid:"");
	}

	/**
	 * Identify a worker process to handle a request for the given session
	 * @param sessionId
	 * @return
	 */
	private IWorkerProcess findWorkerProcess(String sessionId) {
		IWorkerProcess wp = null;
		if (sessionId!=null) {
			wp = this.sessionManager.getSessionWorker(sessionId);
			if (wp!=null && wp.isRunning()) {
				if (LOG.isLoggable(Level.FINER)) {
					LOG.finer("Identified worker "+wp+" for session "+sessionId);
				}
				return wp;
			}
		}
		// works only on home currently!
		wp = IComponentsLookup.INSTANCE.lookup(targetWorkerProcessComponent,IWorkerProcess.class);
		if (wp!=null && !wp.isRunning()) {
			if (LOG.isLoggable(Level.FINER)) {
				LOG.finer("Latest gateway web worker is not up (yet). Searching for last good from detached");
			}
			// not running (yet). Try to find youngest worker that is either detached or running
			Map<String,IWorkerProcess> wm = IComponentsLookup.INSTANCE.lookup(targetWorkerProcessComponent,IWorkerHorde.class).getWorkerProcesses();
			long c = 0;
			for (Map.Entry<String,IWorkerProcess> e : wm.entrySet()) {
				IWorkerProcess w = e.getValue();
				int s = w.getState();
				if (s == IWorkerProcess.STARTED) {
					// it's up now
					return w;
				}
				if (s == IWorkerProcess.DETACHED) {
					if (c<w.getDetachTime()) {
						wp = w;
						c  = w.getDetachTime();
					}
				}
			}
			if (wp!=null) {
				if (LOG.isLoggable(Level.FINER)) {
					LOG.finer("Identified "+wp.getName()+" as next best match");
				}
			}
		}
		return wp;
	}

	private String getSessionId(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies!=null && cookies.length>0) {
			for (Cookie c : cookies)  {
				if ("JSESSIONID".equals(c.getName())) {
					String sessionCookieString = c.getValue();
					if (sessionCookieString!=null) {
						// jetty may append a "."+node name to better support stickyness. This is not part of the internal
						// session name so we need to removed it
						int p=sessionCookieString.lastIndexOf('.');
						if (p>=0) {
							sessionCookieString=sessionCookieString.substring(0, p);
						}
					}
					return sessionCookieString;
				}
			}
		}
		return null;
	}

}
