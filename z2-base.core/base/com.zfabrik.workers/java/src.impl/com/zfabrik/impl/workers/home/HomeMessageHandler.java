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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.sync.SynchronizationRunner;
import com.zfabrik.work.ApplicationThreadPool;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IHomeLayout;

/**
 * Receives messages from worker processes and (if so needed) forwards.
 */
public class HomeMessageHandler implements IMessageHandler {
	private final static Logger LOG = Logger.getLogger(HomeMessageHandler.class.getName());

	public final static String CMDGROUP = "com.zfabrik.workers.home";

	public final static String CMD_INVALIDATION = "invalidation";
	public final static String PARAM_RESOURCES = "res";
	public final static String PARAM_SCOPE = "scope";
	public final static String PARAM_WORKER = "worker";
	public final static String CMD_SYNC = "synchronize";
	public final static String CMD_VERIFY = "verify";
	public final static String CMD_GETLOG = "getSyncLog";
	public final static String RES_LOG = "log";

	private SynchronizationRunner sr;

	@SuppressWarnings("unchecked")
	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		String command = (String) args.get(IMessageHandler.COMMAND);
		boolean invalidation = false;
		if ((invalidation = CMD_SYNC.equals(command))	|| (CMD_VERIFY.equals(command))) {

			if (invalidation) {
				final int scope = (Integer) args.get(PARAM_SCOPE);
				if ((scope & IHomeLayout.SCOPE_HOME) != 0) {
					// run synchronization in this home
					SynchronizationRunner r = new SynchronizationRunner(SynchronizationRunner.INVALIDATE_AND_VERIFY);
					r.setKeepLog(true);
					r.execute(false);
					synchronized (this) {
						this.sr = r;
					}
				}
			} else {
				// run a verification
				SynchronizationRunner r = new SynchronizationRunner(
						SynchronizationRunner.VERIFY_ONLY);
				r.setKeepLog(true);
				r.execute(false);
				synchronized (this) {
					this.sr = r;
				}
			}
		} else if (CMD_GETLOG.equals(command)) {
			Map<String, Serializable> res = new HashMap<String, Serializable>();
			synchronized (this) {
				if (this.sr != null) {
					res.put(RES_LOG, (Serializable) new ArrayList<String>(this.sr.getLog()));
				}
			}
			return res;
		} else if (CMD_INVALIDATION.equals(command)) {
			final Collection<String> invs = (Collection<String>) args.get(PARAM_RESOURCES);
			final int scope = (Integer) args.get(PARAM_SCOPE);
			final String worker = (String) args.get(PARAM_WORKER);
			// never called in app thread --> never wait
			ApplicationThreadPool.instance().execute(false, new Runnable() {
				public void run() {
					try {
						IHomeLayout.get().broadcastInvalidations(invs, -1, scope, worker);
					} catch (IOException e) {
						LOG.log(Level.WARNING,
								"Error during broadcast of invalidations", e);
					}
				}
			});
		}
		return null;
	}

}
