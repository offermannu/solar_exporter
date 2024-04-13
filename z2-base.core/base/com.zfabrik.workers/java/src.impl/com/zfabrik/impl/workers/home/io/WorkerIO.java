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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.impl.workers.StreamReader;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * @author hb
 * 
 */
public class WorkerIO {
	private String id;
	private StreamReader stdoutreader, stderrreader;
	private StdOutHandler outHandler;
	private StdErrHandler errHandler;
	private Logger logger;

	/**
	 * @param id
	 * @param process
	 */
	public WorkerIO(String id, Process process, IWorkerProcess wp) {
		String name = id;
		this.id = id;
		this.logger = Logger.getLogger(this.getClass().getName() + "." + name);
		Logger tunnel_out = Logger.getLogger(this.getClass().getName() + "." + name + ".1"); 
		Logger tunnel_err = Logger.getLogger(this.getClass().getName() + "." + name + ".2"); 
		Logger logger = Logger.getLogger(this.getClass().getName() + "." + name); 
		this.outHandler = new StdOutHandler(process.getOutputStream(), id, tunnel_out,logger, wp);
		this.errHandler = new StdErrHandler(id, tunnel_err, tunnel_err, wp);
		this.stdoutreader = new StreamReader(new InputStreamReader(process.getInputStream()), logger, this.outHandler);
		this.stderrreader = new StreamReader(new InputStreamReader(process.getErrorStream()), logger, this.errHandler);

		new Thread(this.stdoutreader, id+"/out").start();
		new Thread(this.stderrreader, id+"/err").start();
	}

	public void decouple() {
		// this makes sure, the readers do not try to invalidate the worker
		// because the streams got lost. This is important so that during
		// process termination we do not force the stream readers to try in
		// parallel
		// and get blocked
		this.errHandler.decouple();
		this.outHandler.decouple();
		// we also decouple the stream readers so they know
		// we are going down
		this.stderrreader.decouple();
		this.stdoutreader.decouple();
	}

	public void close(boolean sync) {
		this.decouple();
		try {
			if (this.stderrreader != null) {
				this.stderrreader.close(sync);
			}
		} catch (Exception e) {
			this.logger.log(Level.WARNING, "Caught exception while trying to close err stream reader for worker " + id, e);
		}
		try {
			if (this.stdoutreader != null) {
				this.stdoutreader.close(sync);
			}
		} catch (Exception e) {
			this.logger.log(Level.WARNING, "Caught exception while trying to close out stream reader for worker " + id, e);
		}
	}

	/**
	 * spend some time waiting for termination
	 * 
	 * @param timeout
	 * @return
	 */
	public void waitFor(long timeout) {
		long start = System.currentTimeMillis();
		long end = start + timeout;
		long to = end - System.currentTimeMillis();
		if (to > 0)
			this.stderrreader.waitFor(to);
		to = end - System.currentTimeMillis();
		if (to > 0)
			this.stdoutreader.waitFor(to);
		this.logger.fine("Worker IO set terminated after " + (System.currentTimeMillis() - start) + "ms wait time (timeout=" + timeout + "ms)");
	}

	public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException {
		return this.outHandler.sendMessage(args, timeout);
	}

}
