/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home.io;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.impl.workers.IStreamEventHandler;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * Simply catches the output and writes it to some collected log...
 * 
 * @author hb
 * 
 */
public class StdErrHandler implements IStreamEventHandler {
	@SuppressWarnings("unused")
	private Logger logger;
	private Logger tunnel;
	private IWorkerProcess wp;

	public StdErrHandler(String id, Logger tunnel, Logger logger, IWorkerProcess wp) {
		this.wp = wp;
		this.tunnel = tunnel;
		this.logger = logger;
	}

	public void process(String line) {
		if (this.tunnel.isLoggable(Level.INFO)) {
			StringBuffer sbuffer = new StringBuffer(line.length() /* + this.prefix.length() */);
			sbuffer.append(line);
			this.tunnel.info(sbuffer.toString());
		}
	}
	
	public synchronized void decouple() {
		this.wp = null;
	}

	public void close() {
		IWorkerProcess p;
		synchronized (this) {
			p = this.wp;
		}
		if (p!=null) {
			p.stop(0);
		}
	}

}
