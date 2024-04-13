/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.worker;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

import com.zfabrik.impl.workers.IStreamEventHandler;
import com.zfabrik.impl.workers.MessageExchange;
import com.zfabrik.work.ApplicationThreadPool;
import com.zfabrik.work.IThreadPool;
import com.zfabrik.work.WorkManager;
import com.zfabrik.workers.IMessageHandler;

/**
 * End point to receive home messages. This handler owns a local thread pool to
 * execute message processing.
 * <p>
 * Message processing is considered infrastructure load. I.e. inherently controlled
 * and limited.
 * <p>
 * In cases where messages translate into application requests, calls should be
 * wrapped in {@link IThreadPool#executeAs(Runnable, boolean)} of {@link ApplicationThreadp}
 *
 * @author hb
 *
 */
public class WorkerEndPoint implements IStreamEventHandler {
	private IMessageHandler handler;
	private OutputStream out;
	private MessageExchange exchange;
	private IThreadPool threadPool;

	/**
	 * @param slave
	 */
	public WorkerEndPoint(IMessageHandler handler, Logger logger) {
		this.handler = handler;
		this.out = System.out;

		// create a thread pool of the same size then the
		// application thread pool
		this.threadPool  = WorkManager.get().createThreadPool(WorkerEndPoint.class.getName());
		this.threadPool.setMaxConcurrency(ApplicationThreadPool.instance().getMaxConcurrency());
		this.exchange = new MessageExchange(logger, this.handler, this.threadPool , this.out);
	}

	//
	// Stream event handler (mediating to message exchange)
	//
	public void process(String line) throws IOException {
		this.exchange.processLine(line);
	}

	public void close() {
		if (this.exchange!=null) {
			MessageExchange x = this.exchange;
			this.exchange = null;
			try {
				x.close();
			} finally {
				WorkManager.get().releaseThreadPool(WorkerEndPoint.class.getName());
			}
		}
	}

	// called from worker process code to send message to other workers via home
	public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException {
		return this.exchange.sendMessage(args, timeout);
	}
}
