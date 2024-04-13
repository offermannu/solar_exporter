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
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

import com.zfabrik.impl.workers.IStreamEventHandler;
import com.zfabrik.impl.workers.MessageExchange;
import com.zfabrik.impl.workers.MessageHandlerResource;
import com.zfabrik.work.ApplicationThreadPool;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;
import com.zfabrik.workers.home.IWorkerProcess;
import com.zfabrik.workers.home.WorkerUtils;

/**
 * Handles the out stream of a worker process, i.e. this is the way back into
 * the master process. For fail-safety, we check for a message boundary in order
 * to seperate arbitrary worker process output from message replies.
 * 
 * @author hb
 * 
 */
public class StdOutHandler implements IStreamEventHandler, IMessageHandler {
	private volatile boolean open = true;
	private Logger logger, tunnel;
//	private String prefix;
	private OutputStream out;
	private MessageExchange exchange;
	private IWorkerProcess wp;

	public StdOutHandler(OutputStream out, String id, Logger tunnel, Logger logger, IWorkerProcess wp) {
		this.out = out;
		this.logger = logger;
		this.tunnel = tunnel;
		this.wp = wp;
//		this.prefix = "&1: ";
		this.exchange = new MessageExchange(logger, this, ApplicationThreadPool.instance() , this.out);
	}

	public boolean isActive() {
		return this.open;
	}

	/**
	 * an unsolicited message has been received. It was not a reply. This method
	 * introspects the message for its type and creates tasks for the
	 * application thread pool of the home process to fullfill the requested
	 * message handling, as there is:
	 * <ul>
	 * <li>Concurrent sending to all worker nodes of the home (see also
	 * {@link IWorkerProcess#MSG_TARGET_ALL}): as concurrently as possible,
	 * all nodes will be notified by forwarding the message as is. A response
	 * will not be provided</li>
	 * <li>Sending a message to a dedicated worker node (see also
	 * {@link IWorkerProcess#MSG_TARGET_NODE}).
	 * </ul>
	 * 
	 * @param message
	 */
	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		// introspect message.
		String target = (String) args.get(IWorkerProcess.MSG_TARGET);
		// we simply preserve the time out and pass it on
		String too = (String) args.get(MessageExchange.TIMEOUT);
		long to = Long.MAX_VALUE;
		if (too != null) {
			to = Long.parseLong((String) too);
		}
		if ((IWorkerProcess.MSG_TARGET_HOME.equals(target))) {
			return MessageHandlerResource.forwardMessageToHandler(logger, args);
		} 
		if ((IWorkerProcess.MSG_TARGET_ALL.equals(target))) {
			IHomeLayout.get().broadcastMessage(args, to); 
		} else if (IWorkerProcess.MSG_TARGET_NODE.equals(target)) {
			String id = (String) args.get(IWorkerProcess.MSG_NODE);
			if (id == null)
				throw new IllegalArgumentException("No target node id specified");
			
			// find target worker process
			IWorkerProcess wp = WorkerUtils.getWorkerProcess(id);
			if (wp==null) {
				throw new IllegalArgumentException("Failed to retrieve worker process ("+id+")");
			}
			if (!wp.isRunning()) {
				throw new IllegalStateException("Worker process not started ("+id+")");
			}
			return wp.sendMessage(args,to);
		} else {
			// discard
			this.logger.severe("Home was asked to distribute a message with unknown target type " + target);
		}
		return null;
	}

	/**
	 * send a message. See {@see MessageExchange} 
	 * @param args
	 * @param timeout
	 * @return
	 * @throws IOException
	 */
	public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException {
		return this.exchange.sendMessage(args,timeout);
	}

	/**
	 * this method gets called asynchronously when there is data available on
	 * the out stream of the child process.
	 */
	public void process(String line) throws IOException {
		if ((line = this.exchange.processLine(line)) != null) {
			this.tunnel.info(/*this.prefix +  */line);
		}
	}
	
	public synchronized void decouple() {
		this.wp = null;
	}


	public void close() {
		this.open = false;
		try {
			this.exchange.close();
		} finally {
			IWorkerProcess p;
			synchronized (this) {
				p = this.wp;
			}
			if (p!=null) {
				p.stop(0);
			}
		}
	}
}
