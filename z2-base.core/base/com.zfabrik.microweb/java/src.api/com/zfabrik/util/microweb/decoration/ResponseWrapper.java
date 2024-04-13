/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.decoration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.zfabrik.util.microweb.util.ServletOutputStreamWrapper;

public class ResponseWrapper extends HttpServletResponseWrapper {
	private ByteArrayOutputStream baOut;
	private ServletOutputStream seOut;
	private PrintWriter writer;
	private boolean invalid = false;

	public ResponseWrapper(HttpServletResponse response) {
		super(response);
	}

	public void setContentType(String type) {
		super.setContentType(type);
		if (logger.isLoggable(Level.FINE)) {
			logger.info("ResponseWrapper.setContentType(" + type + ")");
		}
		if (writer == null) {
			if ((type != null) && (type.indexOf("text/html") >= 0)) {
				// we will catch it!
				this.baOut = new ByteArrayOutputStream(10 * 1024);
				this.seOut = new ServletOutputStreamWrapper(this.baOut);
				try {
					this.writer = new PrintWriter(new OutputStreamWriter(this.baOut, getCharacterEncoding()));
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (writer != null) {
			return this.writer;
		} else {
			return super.getWriter();
		}
	}

	public String getContent() {
		try {
			this.writer.flush();
			this.seOut.flush();
			return new String(this.baOut.toByteArray(), getCharacterEncoding());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isValid() {
		return (!this.invalid) && (this.writer != null);
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (this.writer != null)
			return this.seOut;
		return super.getOutputStream();
	}

	public void sendError(int sc) throws IOException {
		this.invalid = true;
		super.sendError(sc);
	}

	public void sendError(int sc, String msg) throws IOException {
		this.invalid = true;
		super.sendError(sc, msg);
	}

	public void sendRedirect(String location) throws IOException {
		this.invalid = true;
		super.sendRedirect(location);
	}
	
	final static Logger logger = Logger.getLogger(ResponseWrapper.class.getName());

}