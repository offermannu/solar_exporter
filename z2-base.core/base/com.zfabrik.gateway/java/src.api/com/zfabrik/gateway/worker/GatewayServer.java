/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.gateway.worker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.ThreadPool;

import com.zfabrik.gateway.GatewayFactory;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.workers.worker.HomeHandle;

/**
 * The Gateway server is a drop-in replacement for the default Jetty Server class for the 
 * Gateway receiving end - on the worker node.
 * 
 * <ul>
 * <li>It computes the HTTP dispatch port and sets it as system property <code>jetty.http.port</code>. It is crucial that the HTTP connector
 * refers to this property in the configuration (as is default).</li>
 * <li>Any Gateway request from the dispatching side, the home process, will be responded with an appendix indicating session id and remaining
 * TTL. This is used to update the worker lease, in case the worker node is detached</li>
 * </ul>
 */
public class GatewayServer extends Server {

	/**
	 * System property set to convey http port to receive internal dispatches
	 */
	public static final String GATEWAY_PORT = "com.zfabrik.gateway.jetty.http.host";


	// when this header is set, the gateway connector sends extra session info as response appendix
	public final static String X_Z2_GATEWAY_REQUEST = "X-Z2-GATEWAY-REQUEST";

	
	private final static Logger LOG = Logger.getLogger(GatewayServer.class.getName());
	private static final String GATEWAY_EXP = "com.zfabrik.gateway/exp";
	private static final String GATEWAY_SID = "com.zfabrik.gateway/sid";
	
	public GatewayServer(InetSocketAddress addr) {
		super(addr);
		init();
	}

	public GatewayServer(int port) {
		super(port);
		init();
	}

	public GatewayServer(ThreadPool pool) {
		super(pool);
		init();
	}

	public GatewayServer() {
		init();
	}
	
	private void init() {
		if (!Foundation.isWorker()) {
			throw new IllegalStateException("The Gateway server is only for worker processes. Possibly you confused Jetty configurations? Please check the z2 wiki!");
		}
		
		// at construction time we compute the gate way port and set it as Jetty system property
		String workerProcessName = System.getProperty(Foundation.PROCESS_WORKER);
		String port = Integer.toString(GatewayFactory.getInfo().getGatewayPort(workerProcessName));
		LOG.info("Setting Jetty HTTP port on worker "+workerProcessName+" to Gateway port "+port);
		
		System.setProperty(
			GATEWAY_PORT, 
			port
		);
	}
	
	
	@Override
	public void handle(HttpChannel connection) throws IOException, ServletException {
		LOG.finer("GatewayServer.handle ("+this+"): Starting");
		HomeHandle.instance().getWorkerLease().increaseLease();

		try {
			final Request request=connection.getRequest();
	        final Response response=connection.getResponse();
		
			String h = request.getHeader(X_Z2_GATEWAY_REQUEST);
			
			super.handle(connection);
			
			if ("1".equals(h)) {
				// compute extra data, the appendix, and pass it on to our connection implementation,
				// so that it can be appended to the actual response.
				// 
				// The following attributes are set in the WebAppContextWrapper. Ugly but true: That's the place we know about these
				// and are able to make it through to here.
				// 
				String sid = (String) request.getAttribute(GATEWAY_SID);
				Long   exp = (Long) request.getAttribute(GATEWAY_EXP);
				String appendix;
				if (sid!=null && exp!=null) {
					LOG.finer("GatewayServer.handle ("+this+"): setting session id "+sid+" and expiration "+new Date(exp));
					// Connection objects are reused
					appendix = sid+"/"+exp.toString();
				} else {
					appendix="";
				}
				response.flushBuffer();
				
				// convey the appendix to the connection
				((GatewayConnection) connection.getEndPoint().getConnection()).setAppendix(appendix);
			}
		} finally {
			HomeHandle.instance().getWorkerLease().decreaseLease();
			LOG.finer("GatewayServer.handle ("+this+"): Done");
		}
	}
	
}
