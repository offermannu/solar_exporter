/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb.util;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class ServletOutputStreamWrapper extends ServletOutputStream {
	private OutputStream out;

	public ServletOutputStreamWrapper(OutputStream out) {
		this.out = out;
	}

	public void write(int b) throws IOException {
		this.out.write(b);
	}
	
	public void write(byte[] b) throws IOException {
		this.out.write(b);
	}
	
	public void write(byte[] b, int off, int len) throws IOException {
		this.out.write(b, off, len);
	}
	
	@Override
	public boolean isReady() {
		return true;
	}
	
	@Override
	public void setWriteListener(WriteListener wl) {
		throw new UnsupportedOperationException();
	}
	
}
