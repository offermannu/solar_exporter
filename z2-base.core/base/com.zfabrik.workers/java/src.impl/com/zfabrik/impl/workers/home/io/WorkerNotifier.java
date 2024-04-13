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

import java.io.Serializable;
import java.util.Map;

import com.zfabrik.workers.home.IWorkerProcess;

/**
 * task that forwards a message to a dedicated worker process
 * @author hb
 */
public class WorkerNotifier implements Runnable {
	private Map<String,Serializable> message;
	private IWorkerProcess process;
	private long timeout;
	private Map<String,Serializable> result;
	private Object mutex = new Object();
	private Throwable error;
	private boolean called = false;
	private boolean killOnTimeOut;
	private String id;
	
	public WorkerNotifier(Map<String,Serializable> args, IWorkerProcess wp,long timeout, boolean killOnTimeOut) {
		this.message = args;
		this.process = wp;
		this.timeout = timeout;
		this.id = wp.getComponentName();
		this.killOnTimeOut = killOnTimeOut;
	}
	
	public void run() {
		synchronized (mutex) {
			try {
				this.called=true;
				if (this.process.getState()==IWorkerProcess.STARTED)
					this.result = this.process.sendMessage(message,timeout,killOnTimeOut);
			} catch (Exception e) {
				this.error = e;
			}			
			mutex.notifyAll();
		}
	}
	
	public Map<String,Serializable> getResult() {
		synchronized (mutex) {
			if (!this.called) {
				try {
					this.mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			}
			return this.result;
		}
	}

	public Throwable getError() {
		synchronized (mutex) {
			if (!this.called) {
				try {
					this.mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException(e);
				}
			}
			return this.error;
		}
	}

	public String getId() {
		return this.id;
	}
}
