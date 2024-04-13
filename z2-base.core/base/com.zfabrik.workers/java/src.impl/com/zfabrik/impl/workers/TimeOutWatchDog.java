/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * helper utility to warn about hanging threads
 * @author hb
 *
 */
public class TimeOutWatchDog {
	
	private class Watch {
		private String title;
//		private Thread thread;
		private long expire;
		private long delay;
	}
	
	private List<Watch> watches = new LinkedList<Watch>();
	
	private class Blamer implements Runnable {
		private boolean stop;
		
		public void run() {
			while (!isStop()) {
				long now = System.currentTimeMillis();
				long d =Long.MAX_VALUE,h;
				Set<Watch> expired = new HashSet<Watch>();
				synchronized (TimeOutWatchDog.this) {
					for (Watch w : TimeOutWatchDog.this.watches) {
						if (w.expire<=now) {
							StringBuilder b = new StringBuilder();
							b.append("TimeOutWatchDog: "+w.title+" expired (delay: "+w.delay+"ms)!\nFull Stack Trace:\n");
							// dump full thread dump
							ThreadMXBean tmb = ManagementFactory.getThreadMXBean();
							if (tmb!=null) {
								for (ThreadInfo ti:tmb.dumpAllThreads(true, true)) {
									b.append(ti.toString());
									b.append("\n");
								}
							}
							logger.warning(b.toString());
							expired.add(w);
						} else {
							h = w.expire-now;
							if (h<d) d=h;
						}
					}
					watches.removeAll(expired);					
				}
				synchronized (this) {
					try {
						this.wait(d);
					} catch (InterruptedException e) {
						logger.warning("INTERRUPTED");
					}
				}
			}
		}
		public synchronized void stop() {
			this.stop=true;
			this.notify();
		}
		public synchronized boolean isStop() {
			return this.stop;
		}
		public synchronized void adjust() {
			this.notify();
		}
	}

	private Blamer blamer = new Blamer();
	
	public TimeOutWatchDog(String title) {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader()); 
		try {
			Thread t = new Thread(this.blamer,"TimeOutWatchDog Blamer: "+title);
			t.setDaemon(true);
			t.start();
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}		
	}
	
	public void stop() {
		this.blamer.stop();
	}
	
	public synchronized Object addWatch(Thread t, long delay, String title) {
		Watch w = new Watch();
//		w.thread = t;
		w.title = title;
		w.expire = System.currentTimeMillis()+delay;
		w.delay = delay;
		this.watches.add(w);
		this.blamer.adjust();
		return w;
	}

	public synchronized void removeWatch(Object w) {
		if (w instanceof Watch) {
			this.watches.remove((Watch) w);
		}
	}
	
	private final static Logger logger = Logger.getLogger(TimeOutWatchDog.class.getName());
}
