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
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnection;

/**
 * We use a special connection implementation so we can intercept the end of transmission and sneak in some extra payload
 * that is consumed by the home {@link GatewayServer} and not sent to the actual client.
 * <p>
 * The extra-data, the appendix, contains the current sessionid and expiration time. We cannot reliably provide this information
 * before the actual request is done, at which point the response may already be committed.
 * </p>
 */
public class GatewayConnection extends HttpConnection {
	private final static Logger LOG = Logger.getLogger(GatewayConnection.class.getName());
	
	private String appendix; 

	public GatewayConnection(HttpConfiguration config, Connector connector, EndPoint endPoint) {
		super(config, connector, endPoint, false);
	}

	public void setAppendix(String appendix) {
		this.appendix = appendix;
		LOG.finer("Setting appendix="+appendix);
	}
	
	@Override
	public void onCompleted() {
		super.onCompleted();
		LOG.finer("onCompleted "+this);
		writeAppendix();
	}

	@Override
	public void onFillable() {
		super.onFillable();
		LOG.finer("onFillable "+this);
	}

	@Override
	public void onOpen() {
		super.onOpen();
		this.appendix=null;
		LOG.finer("onOpen "+this);
	}
	
	private void writeAppendix() {
		if (this.appendix==null) {
			return;
		}
		try {
			// send the appendix
			ByteBuffer b = ByteBuffer.allocate(appendix.getBytes().length+1);
			b.put(appendix.getBytes());
			b.put((byte)10);
			b.flip();
			LOG.finer("Appendix buffer contains "+b.limit()+ " bytes");
			boolean flushed = false;
			int l=0;
			while (!flushed) {
				int p = b.position();
				flushed=this.getEndPoint().flush(b);
				int d = b.position()-p;
				l+=d;
				if (LOG.isLoggable(Level.FINEST)) {
					LOG.finest("Flushed "+d+" bytes.");
				}
			}
					
			LOG.finer("Wrote "+l+" appendix bytes");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
	}
}
