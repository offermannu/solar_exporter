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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpVersion;
import org.apache.http.impl.io.AbstractSessionOutputBuffer;
import org.apache.http.impl.io.HttpRequestWriter;
import org.apache.http.io.HttpMessageWriter;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicLineFormatter;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import com.zfabrik.gateway.worker.GatewayServer;

/**
 * Marshalling a servlet request into its original wire format - and supporting streaming. This is used
 * to forward an incoming request to a worker node.
 */
public class RequestMarshaller  {
	private final static int CHUNK = 16384;
	private static HttpParams params = new BasicHttpParams();
	private HttpServletRequest request;
	private int state = 0; // 0=headers, 1=body, 2=done
	
	public RequestMarshaller(HttpServletRequest request) throws IOException {
		this.request = request;
	}

	
	/**
	 * provide a chunk of the request data. 
	 * 
	 * @return
	 */
	public byte[] getChunk() throws Exception {
		byte [] headers=null;
		if (state==0) {
			// write headers into byte array
			headers=headersToBytes();
			// switch to body
			state=1;
			// fall through - in case there is still room
		}
		if (state==1) {
			InputStream in = this.request.getInputStream();
			if (in!=null) {
				int top = (headers!=null? headers.length : 0);
				int max = CHUNK-top;
				if (max>0) {
					byte[] body = new byte[max];
					int l = in.read(body);
					if (l>0) {
						// simple case
						if (top==0 && l==max) {
							return body;
						}
						// else concatenate
						byte[] r = new byte[top+l];
						if (headers!=null) {
							System.arraycopy(headers, 0, r, 0, top);
						}
						System.arraycopy(body, 0, r, top, l);
						return r;
					} else {
						// done
						state=2;
					}
				}
			} else {
				// done
				state=2;
			}
		}
		// in case we have the headers, return those
		if (headers!=null) {
			return headers;
		}
		return null;
	}
	
	private byte[] headersToBytes() throws Exception {
		// fill headers and stuff into initial buffer. Must fit
		StringBuilder sb = new StringBuilder(255);
		sb.append(this.request.getRequestURI());
		if (this.request.getQueryString()!=null) {
			sb.append("?").append(this.request.getQueryString());
		}
		BasicHttpRequest req = new BasicHttpRequest(this.request.getMethod(), sb.toString(), HttpVersion.HTTP_1_1);
		
		Enumeration<String> hn = this.request.getHeaderNames();
		while (hn.hasMoreElements()) {
			String h = hn.nextElement();
			Enumeration<String> hv = this.request.getHeaders(h);
			while (hv.hasMoreElements()) {
				req.addHeader(h,hv.nextElement());
			}
		}
		req.addHeader(GatewayServer.X_Z2_GATEWAY_REQUEST, "1");
		
		OutBuffer out = new OutBuffer();
		HttpMessageWriter w = new HttpRequestWriter(out, new BasicLineFormatter(), null);
		w.write(req);
		out.flush();
		return out.getBaOut().toByteArray();
	}

	/****************************
	 * Write abstract request into byte array using apache commons http components  
	 * @author hb
	 *
	 *****************************/

	// helper class to write into byte buffer
	private class OutBuffer extends AbstractSessionOutputBuffer {
		private ByteArrayOutputStream baOut = new ByteArrayOutputStream();
		public OutBuffer() {
			init(baOut,4096,params);
		}

		public ByteArrayOutputStream getBaOut() {
			return baOut;
		}
	}
	

	// write as byte array buffer
	public byte[] toByteArray() throws Exception {
		StringBuilder sb = new StringBuilder(255);
		sb.append(this.request.getRequestURI());
		if (this.request.getQueryString()!=null) {
			sb.append("?").append(this.request.getQueryString());
		}
		BasicHttpRequest req = new BasicHttpRequest(this.request.getMethod(), sb.toString(), HttpVersion.HTTP_1_1);
		
		Enumeration<String> hn = this.request.getHeaderNames();
		while (hn.hasMoreElements()) {
			String h = hn.nextElement();
			Enumeration<String> hv = this.request.getHeaders(h);
			while (hv.hasMoreElements()) {
				req.addHeader(h,hv.nextElement());
			}
		}
		OutBuffer out = new OutBuffer();
		HttpMessageWriter w = new HttpRequestWriter(out, new BasicLineFormatter(), null);
		w.write(req);
		out.flush();
		InputStream in = this.request.getInputStream();
		if (in!=null) {
			// append
			byte[] buffer = new byte[16384];
			int l;
			while ((l=in.read(buffer))>=0) {
				out.getBaOut().write(buffer,0,l);
			}
		}
		return out.getBaOut().toByteArray();
	}
	
}
