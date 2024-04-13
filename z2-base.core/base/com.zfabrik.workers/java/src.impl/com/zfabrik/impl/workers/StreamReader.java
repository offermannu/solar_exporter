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

import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author hb
 * 
 * Working with ordinary streams is not working properly, since sometimes you
 * cannot close it (blocks) or the reading thread cannot be interrupted.
 * <ol>
 * <li>http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4514257</li>
 * </ol>
 * 
 */
public class StreamReader implements Runnable {
	private final static String[] STATES = new String[] { "STOPPING", "INACTIVE", "RUNNING", "DECOUPLED" };
	private final static short STOPPING = 0;
	private final static short INACTIVE = 1;
	private final static short RUNNING = 2;
	private final static short DECOUPLED = 3;
	// We keep a normal buffer of this size
	// anything larger will be released again immediately
	private final static int INITIAL_BUFFER_SIZE = 1024;
	// At this many chars we wrap lines 
	private final static int MAX_BUFFER_SIZE = 10*1024*1024;
	private Reader in;
	private short state = INACTIVE;
	private Thread thread;
	private IStreamEventHandler handler;
	private Logger logger;
	private boolean processing = false;
	private Object lock = new Object();

	/**
	 * @param in
	 * @param logger
	 * @param pcr
	 */
	public StreamReader(Reader in, Logger logger, IStreamEventHandler handler) {
		super();
		this.in = in;
		this.handler = handler;
		this.logger = logger;
	}

	private short _state() {
		synchronized (this.lock) {
			return this.state;
		}
	}

	private short _state(short ns) {
		synchronized (this.lock) {
			short h = this.state;
			this.state = ns;
			return h;
		}
	}

	public void run() {
		long count = 0;
		try {
			this.thread = Thread.currentThread();
			_state(RUNNING);
			int c = -2;
			StringBuilder buffer = new StringBuilder(INITIAL_BUFFER_SIZE);
			while ((_state() >= RUNNING) && ((c = this.in.read()) >= 0)) {
				count++;
				if (c == 10 || buffer.length()>=MAX_BUFFER_SIZE) {
					try {
						synchronized (this.lock) {
							this.processing = true;
						}
						this.handler.process(buffer.toString());
					} catch (Exception e) {
						if (this.logger != null)
							this.logger.log(Level.WARNING, "Error in line processing of handler", e);
					} finally {
						synchronized (this.lock) {
							this.processing = false;
						}
					}
					if (buffer.length()>INITIAL_BUFFER_SIZE) {
						// release large buffer
						buffer = new StringBuilder(INITIAL_BUFFER_SIZE);
					} else {
						// Reuse buffer
						buffer.setLength(0);
					}
				} else
				if (c==13) {
					// ignore
				} else {
					buffer.append((char) c);
				}
			}
			if (_state()==RUNNING) {
				this.logger.warning("Unexpected end of worker stream!");
			}
			if (buffer.length() > 0) {
				// last line
				try {
					this.handler.process(buffer.toString());
				} catch (Exception e) {
					if (this.logger != null)
						this.logger.log(Level.WARNING, "Error in line processing of handler", e);
				}
			}
			if (this.logger != null)
				this.logger.fine("Left stream reader loop in state " + STATES[this.state] + " last char read=" + c);
		} catch (Exception e) {
			if (_state() >= RUNNING) {
				// otherwise ignore
				if (this.logger != null)
					this.logger.log(Level.WARNING, "Problem reading stream", e);
			} else if (_state() == STOPPING) {
				if (this.logger != null)
					this.logger.finer("Caught exception in stopping state: process probably terminated orderly");
			}
		} finally {
			_state(INACTIVE);
			try {
				this.handler.close();
			} catch (Exception e) {
				if (this.logger != null)
					this.logger.log(Level.WARNING, "Caught exception during close() call of stream event handler", e);
			} finally {
				synchronized (this.lock) {
					this.lock.notifyAll();
				}
				if (this.logger != null)
					this.logger.fine("Stream reader exiting after having read " + count + " characters");
			}
		}
	}

	public final void close(boolean sync) {
		synchronized (this.lock) {
			if (this.state >= RUNNING) {
				if (this.logger != null)
					this.logger.fine("Stopping stream reader");
				this.state = STOPPING;
				try {
					// bad trick... but on <=1.4.2 this seems to be the way..
					this.in.close();
				} catch (IOException ioe) {
					if (this.logger != null)
						this.logger.log(Level.WARNING, "Exception during stop-close of in stream", ioe);
				}
				if (this.logger != null)
					this.logger.finer("Interrupting reader thread!");
				if (!this.processing) {
					// interrupting the reader thread is a way of kicking it out
					// of dizzyness
					this.thread.interrupt();
				}
			}
		}
		if (sync) {
			try {
				synchronized (this.lock) {
					while (this.state == STOPPING) {
						if (!this.processing) {
							if (this.logger != null)
								this.logger.fine("Interrupting reader thread!");
							this.thread.interrupt();
						}
						this.lock.wait(200);
					}
				}
			} catch (InterruptedException e) {
				if (this.logger != null)
					this.logger.log(Level.WARNING, "Stream reader stop interrupted!", e);
			}
		}
	}

	public void waitFor(long timeout) {
		synchronized (this.lock) {
			if (this.state != INACTIVE) {
				try {
					long start = System.currentTimeMillis();
					this.lock.wait(timeout);
					if (this.logger != null)
						this.logger.fine("Stream reader terminated after " + (System.currentTimeMillis() - start) + "ms wait time");
				} catch (InterruptedException e) {
					if (this.logger != null)
						this.logger.log(Level.WARNING, "Wait for termination interrupted", e);
				}
			}
		}
	}

	public void decouple() {
		synchronized (this.lock) {
			this.state = DECOUPLED;
		}
	}

}
