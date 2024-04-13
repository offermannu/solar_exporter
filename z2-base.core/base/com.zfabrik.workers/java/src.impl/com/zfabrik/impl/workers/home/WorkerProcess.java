/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;
import javax.management.StandardMBean;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.impl.workers.MessageExchangeClosed;
import com.zfabrik.impl.workers.MessageTimeOutException;
import com.zfabrik.impl.workers.WorkerConstants;
import com.zfabrik.impl.workers.home.io.WorkerIO;
import com.zfabrik.util.internal.WorkerVault;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IWorkerHorde;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * An instance of this class represents a z2 worker process to the &lt;home&gt; process.
 * <p>
 * Worker processes apart from being startable and stoppable can be detached.
 * Detached means that the process is being held in work until a new process
 * has been lauchned in lieu of the previous one. Any previous one
 * will stay in service depending on an application level defined condition. For example open
 * web application sessions.
 * When that condition is no longer met, the worker will terminate itself.
 * <p>
 * Worker processes are named &lt;componentName&gt;@&lt;time stamp&gt;
 * <p>
 * In order to retrieve the latest active worker process represented by this component
 * look up as {@link IWorkerProcess}. The latest active is always the latest non-detached
 * worker process. If there is only detached processes, the latest may not have been started
 * even.
 * <p>
 * In order to find out about all worker processes maintained for this component, look up
 * the {@link IWorkerHorde} interface.
 *
 * @author hb
 *
 */
public class WorkerProcess implements IWorkerProcess {

	private int variantNum;

	private Date lastStarted;
	private Date lastAttempted;
	private Date detach;
	private Throwable lastError;
	private int numSuccess;
	private int numAttempt;

	private WPMBean wpb = new WPMBean();

	// the set of all non-detached, running processes
	// that participate in maintenance communication (e.g. home handle broadcasts)
	private static Set<IWorkerProcess> workerProcesses = new HashSet<IWorkerProcess>();

	public static void workerStarted(IWorkerProcess wp) {
		synchronized (workerProcesses) {
			workerProcesses.add(wp);
		}
	}

	public static void workerDetached(IWorkerProcess wp) {
		synchronized (workerProcesses) {
			workerProcesses.remove(wp);
		}
	}

	public static Set<IWorkerProcess> allWorkers() {
		synchronized (workerProcesses) {
			return new HashSet<IWorkerProcess>(workerProcesses);
		}
	}

	private String componentName,name;
	private short state;
	private IComponentDescriptor desc;
	private long to_start, to_term, to_msg;
	private Process pc;
	private WorkerIO workerIO;
	private ObjectName on;
	private Object startStopLock = new Object();
	private String pid;
	private long created;

	public WorkerProcess(String componentName, IComponentDescriptor desc, int variantNum) {
		this.variantNum = variantNum;
		this.created = System.currentTimeMillis();
		this.name = componentName + "@" + variantNum;
		this.componentName = componentName;
		this.desc = desc;
	}

	public long getCreated() {
		return created;
	}

	@Override
	public String getComponentName() {
		return this.componentName;
	}

	/*
	 * send a message from home to this worker
	 */
	public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout,boolean killOnTimeOut) throws IOException {
		if (this.getState() < STARTED)
			throw new IllegalStateException("Worker process is not running: " + this.name);
		if (timeout<0) timeout = to_msg;
		Map<String, Serializable> result = null;
		boolean success = false;
		try {
			result = this.workerIO.sendMessage(args, timeout);
			success = true;
		} catch (MessageTimeOutException mtoe) {
			success = !killOnTimeOut;
			throw mtoe;
		} catch (RemoteException re) {
			// forward remote exceptions!
			success=true;
			throw new RuntimeException("Exception during message handling in worker process: "+this.name,((RemoteException)re).getCause());
		} catch (MessageExchangeClosed mc) {
			success = true;
			logger.warning("Process not running anymore (cannot send message): " + this.name);
			stop(0);
			throw mc;
		} finally {
			if (!success) {
				// there must have been a critical local or wire error
				logger.log(Level.WARNING, "Communication error before or during communication with worker process  (will kill process!): " + this.name);
				stop(0);
			}
		}
		return result;
	}

	public Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException {
		return this.sendMessage(args, timeout,false);
	}

	public void start() {
		synchronized (this.startStopLock) {
			if (this.getState() == NOT_STARTED) {
				if (this.on == null) {
					try {
						this.on = ObjectName.getInstance("zfabrik:type="+WorkerProcessResource.class.getName()+",name=" + this.name);
						ManagementFactory.getPlatformMBeanServer().registerMBean(
								new StandardMBean(this.wpb, WorkerProcessMBean.class),
								this.on);
					} catch (Exception e) {
						throw new IllegalStateException("Error during MBean registration ("+this.on+"): "+this.name,e);
					}
				}
				try {
					this.setState(STARTING);
					// reset start time
					this.lastAttempted=new Date();
					this.lastStarted=null;
					this.detach=null;
					try {
						this.to_start = Long.parseLong(this.desc.getProperties().getProperty(IWorkerProcess.TO_START, IWorkerProcess.TO_START_DEF).trim());
					} catch (Exception e) {
						throw new IllegalStateException("Failed to read process start timeout (" + IWorkerProcess.TO_START + "): " + this.name);
					}
					try {
						this.to_term = Long.parseLong(this.desc.getProperties().getProperty(IWorkerProcess.TO_TERM, IWorkerProcess.TO_TERM_DEF).trim());
					} catch (Exception e) {
						throw new IllegalStateException("Failed to read process termination timeout (" + IWorkerProcess.TO_TERM + "): " + this.name);
					}
					try {
						this.to_msg = Long.parseLong(this.desc.getProperties().getProperty(IWorkerProcess.TO_MSG, IWorkerProcess.TO_MSG_DEF).trim());
					} catch (Exception e) {
						throw new IllegalStateException("Failed to read process communication timeout (" + IWorkerProcess.TO_MSG + "): " + this.name);
					}

					// compute command line
					CommandLineBuilder b = CommandLineBuilder.fromConfig(componentName, variantNum, desc.getProperties());

					String[] args = b.toStringArray();
					ProcessBuilder pb = new ProcessBuilder(args);
					// now we have a command line
					if (logger.isLoggable(Level.FINE)) {
						logger.fine("Starting worker process (with args " + String.join(" ", args) + "): " + this.name);
					}
					try {
						this.pc = WorkerVault.INSTANCE.exec(pb);
					} catch (IOException e) {
						throw new RuntimeException("Failed to start worker process: " + this.name, e);
					}
					this.workerIO = new WorkerIO(this.name, this.pc, this);
					this.pid=Long.toString(this.pc.pid());
					// ping the process to see it is up
					_ping(to_start);
					this.setState(STARTED);
					workerStarted(this);
					this.lastError = null;
				} catch (RuntimeException e) {
					synchronized (this) {
						this.lastError = e;
					}
					throw e;
				} finally {
					WorkerVault.INSTANCE.release(this.pc);
					synchronized (this) {
						this.numAttempt++;
						if (this.state==STARTED) {
							this.numSuccess++;
							this.lastStarted = new Date();
						}
						if (this.state!=STARTED) this.state=NOT_STARTED;
					}
				}
			}
		}
	}

	private void _ping(long timeout) {
		// send ping and wait for response
		Map<String, Serializable> args = new HashMap<String, Serializable>();
		args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_PING);
		try {
			Map<String, Serializable> res  = new HashMap<String, Serializable>();
			res = this.workerIO.sendMessage(args, timeout);
			if (res!=null && !"doki".equals(res.get("oki"))) {
				throw new IllegalStateException("Worker did not respond with acknowledgement upon first ping");
			}
		} catch (MessageExchangeClosed mc) {
			_kill();
			throw new IllegalStateException("Process connection closed (killed process): " + this.name);
		} catch (MessageTimeOutException mtoe) {
			_kill();
			throw new IllegalStateException("Process not responding in configured timeout (" + timeout + "ms). killed the process: "
					+ this.name);
		} catch (Exception e) {
			_kill();
			throw new IllegalStateException("Error during PING of process (killed): " + this.name, e);
		}
	}

	public void stop() {
		stop(to_term);
	}

	private void _kill() {
		synchronized (this.startStopLock) {
			try {
				if (this.getState() > NOT_STARTED) {
					logger.fine("Destroying worker process: " + this.name);
					this.workerIO.decouple();
					this.pc.destroy();
					// note: we do not close the streams yet, as that leads to
					// some sort of dead-lock between terminating and streams not
					// read or so (observed in combination with hanging Zookeeper shutdown
					// threads in the child process)
					while (true) {
						try {
							// give it another 100 ms.
							Thread.sleep(100);
							this.pc.exitValue();
							break;
						} catch (Exception e) {
							// continue
							try {
								if (!_killHard()) {
									logger.info("Waiting for worker process "+name+" to terminate...");
									this.pc.destroy();
								}
							} catch (Exception ex) {
								logger.log(Level.WARNING,"Error during repeated destroy: "+ex);
							}
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e1) {
								logger.log(Level.WARNING,"Worker process termination wait interrupted",e1);
								break;
							}
						}
					}
					this.workerIO.close(false);
					logger.info("Worker process terminated: " + this.name);
				}
			} finally {
				this.setState(NOT_STARTED);
				this.workerIO=null;
				this.pid=null;
				if (this.on!=null) {
					try {
						ManagementFactory.getPlatformMBeanServer().unregisterMBean(this.on);
					} catch (Exception e) {
						logger.log(Level.SEVERE,"Error during unregister of MBean ("+this.on+"): "+this.name,e);
					} finally {
						this.on =null;
					}
				}
			}
		}
	}

	private boolean _killHard() throws Exception {
		if (this.pid!=null) {
			// kill the hard way
			logger.info("Killing worker process "+name+" with pid "+this.pid);
			this.pc.destroyForcibly();
			return true;
		}
		return false;
	}

	public void stop(long to) {
		workerDetached(this);

		// make sure the stream handling tries not to stop this process because
		// the streams got lost, since we are trying to stop
		if (this.workerIO!=null) this.workerIO.decouple();
		synchronized (this) {
			if ((this.state > NOT_STARTED) && (this.state<STOPPING)) {
				this.state = STOPPING;
			} else {
				return;
			}
		}
		if (to > 0) {
			Map<String, Serializable> args = new HashMap<String, Serializable>();
			args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_TERM);
			long start = System.currentTimeMillis();
			long end = System.currentTimeMillis() + to;
			try {
				logger.fine("Sending termination request (timeout=" + to + "ms): " + this.name);
				this.sendMessage(args, to);
				logger.fine("Returned from termination request after " + (System.currentTimeMillis() - start) + "ms (timeout=" + to + "ms): "
						+ this.name);
			} catch (MessageExchangeClosed mec) {
				// already gone - most likely because the streams have closed which is most likely
				// due to early death of the worker process
			} catch (Exception e) {
				logger.log(Level.WARNING, "Exception during orderly worker termination (will really kill now!)", e);
			} finally {
				to = end - System.currentTimeMillis();
				try {
					if (to > 0) {
						logger.fine("Waiting for worker termination (timeout=" + to + "ms): " + this.name);
						this.workerIO.waitFor(to);
					}
				} finally {
					_kill();
				}
			}
		}
		_kill();
	}

	public synchronized void detach() {
		try {
			logger.info("Detaching worker process "+this);
			Map<String, Serializable> args = new HashMap<String, Serializable>();
			args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_DETACH);
			this.sendMessage(args,-1,true);
			this.setState(DETACHED);
			this.detach = new Date();
			workerDetached(this);

			// start a detach reaper thread that checks the lease of the worker
			// and actively kills the worker if it does not come down on its own.
			final WorkerProcess that = this;
			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean stop = false;
					boolean kill = false;
					while (!stop && !kill && that.getState() == DETACHED) {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							// guess that means we should get out of here
							stop = true;
						}
						if (that.getState() == DETACHED) {
							try {
								// call worker to check whether it's lease is still ok
								Map<String, Serializable> args = new HashMap<String, Serializable>();
								args.put(IMessageHandler.COMMAND, WorkerConstants.COMMAND_GETLEASE);
								args = that.sendMessage(args, -1, true);
								Integer l = (Integer) args.get("lease");
								if (l==null || l==1) {
									// lease == 1 means it is only our message keeping it alive!
									logger.log(Level.SEVERE,"Detected expired lease count. Terminating "+that.name);
									kill = true;
								}
							} catch (Exception e) {
								logger.log(Level.SEVERE,"Error when querying lease count. Terminating "+that.name,e);
								kill = true;
							}
						}
					}
					if (kill) {
						that.stop(to_term);
					}
				}
			}, "Worker "+this.name+" reaper").start();

		} catch (Exception e) {
			throw new RuntimeException("Failed to detach process: "+this.name,e);
		}
	}

	@Override
	public synchronized long getDetachTime() {
		if (this.detach!=null) {
			return this.detach.getTime();
		}
		return -1;
	}

	@Override
	public synchronized String getName() {
		return this.name;
	}

	public synchronized short getState() {
		return this.state;
	}

	public synchronized void setState(short state) {
		this.state = state;
	}

	public boolean isRunning() {
		return getState()==STARTED || getState()==DETACHED;
	}

	public boolean isDetached() {
		return getState()==DETACHED;
	}

	public Process test_getProcess() {
		return this.pc;
	}

	public String toString() {
		return this.name+" ("+this.pid+")";
	}


	//
	// MBEAN reporting, synchronized on this!
	//
	private class WPMBean implements WorkerProcessMBean {

		@Override
		public String getComponentName() {
			return WorkerProcess.this.getComponentName();
		}

		@Override
		public short getState() {
			return WorkerProcess.this.getState();
		}

		public Throwable getLastError() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.lastError;
			}
		}

		public synchronized Date getLastSuccessfulStart() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.lastStarted;
			}
		}

		public synchronized Date getLastAttemptedStart() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.lastAttempted;
			}
		}

		public synchronized Date getDetachTime() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.detach;
			}
		}

		public synchronized int getNumberAttemptedStarts() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.numAttempt;
			}
		}

		public synchronized int getNumberSuccesfulStarts() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.numSuccess;
			}
		}

		public synchronized long ping() {
			long start = System.currentTimeMillis();
			WorkerProcess.this._ping(30000);
			return System.currentTimeMillis()-start;
		}

		public void detach() {
			WorkerProcess.this.detach();
		}

		public void stop() {
			WorkerProcess.this.stop();
		}

		public void restart() {
			WorkerProcess.this.stop();
			WorkerProcess.this.start();
		}

		public String getName() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.name;
			}
		}

		@Override
		public String getPID() {
			synchronized (WorkerProcess.this) {
				return WorkerProcess.this.pid;
			}
		}
	}
	//
	// MBEAN reporting
	//

	private final static Logger logger = Logger.getLogger(WorkerProcess.class.getName());
}
