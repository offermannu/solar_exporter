/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.tests;

import java.io.IOException;
import java.io.OutputStream;


public class LineParsingStream extends OutputStream {
	private ILineHandler lineHandler;
	private StringBuilder sb = new StringBuilder();
	
	public void setLineHandler(ILineHandler lineHandler) {
		this.lineHandler = lineHandler;
	}
	
	private void handleLine() {
		this.lineHandler.processLine(sb.toString());
		sb.setLength(0);
	}

	@Override
	public synchronized void write(int c) throws IOException {
		if (c==13) {
			// ignore
		} else
		if (c==10) {
			handleLine();
		} else {
			sb.append((char)c);
		}
	}
	
	@Override
	public synchronized void write(byte[] b, int off, int len) throws IOException {
		super.write(b, off, len);
	}
	
	@Override
	public synchronized void write(byte[] b) throws IOException {
		super.write(b);
	}
}
