/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway.home;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zfabrik.workers.home.IWorkerProcess;

/**
 * Mapping session ids to worker processes, so we can keep dispatching to the right nodes.
 */
public class GatewaySessionManager {
	private static final int CLEANUP_THRESHHOLD = 50;
	private Map<GatewaySessionId, IWorkerProcess> sessions = new HashMap<GatewaySessionId, IWorkerProcess>();
	private int cleanUpCount=CLEANUP_THRESHHOLD;
	
	/**
	 * Get the worker for a sessionId. If there is none, or none running, return <code>null</code>
	 * @param sessionId
	 * @return
	 */
	public synchronized IWorkerProcess getSessionWorker(String sessionId) {
		this._cleanUp();
		GatewaySessionId sid = new GatewaySessionId(sessionId, 0);
		IWorkerProcess wp = this.sessions.get(sid);
		if (wp!=null && !wp.isRunning()) {
			logger.info("Removing worker for sessionid "+sessionId+" because it is not running anymore. Now holding "+this.sessions.size()+" sessions.");
			this.sessions.remove(sid);
			return null;
		}
		return wp;
	}
	
	public int getSize() {
		return this.sessions.size();
	}
	
	public synchronized void updateSession(String sessionId, Long expiration, IWorkerProcess wp) {
		this._cleanUp();
		if (sessionId!=null && expiration!=null) {
			long now = System.currentTimeMillis();
			if (expiration>now) {
				IWorkerProcess wpOld = this.sessions.put(new GatewaySessionId(sessionId, expiration),wp);
				if (wpOld!=null) {
					if (logger.isLoggable(Level.FINE)) {
						logger.fine("Updated worker "+wp+" for sessionid "+sessionId+" w/ expiration "+new Date(expiration)+". Now holding "+this.sessions.size()+" sessions.");
					}
				} else {
					logger.info("Set worker "+wp+" for sessionid "+sessionId+" w/ expiration "+new Date(expiration)+". Now holding "+this.sessions.size()+" sessions.");
				}
			} else {
				IWorkerProcess wpOld = this.sessions.remove(new GatewaySessionId(sessionId, 0));
				if (wpOld!=null && logger.isLoggable(Level.INFO)) {
					logger.info("Removing worker "+wpOld+" for sessionid "+sessionId+" because the lease is outdated. Now holding "+this.sessions.size()+" sessions.");
				}
			}
		}
	}
	
	private void _cleanUp() {
		if (--cleanUpCount<=0) {
			this.cleanUpCount = CLEANUP_THRESHHOLD;
			int expcount=0;
			int deccount=0;
			long now = System.currentTimeMillis();
			Iterator<Map.Entry<GatewaySessionId, IWorkerProcess>> i = this.sessions.entrySet().iterator();
			while (i.hasNext()) {
				Map.Entry<GatewaySessionId, IWorkerProcess> e = i.next();
				long exp = e.getKey().getExpiration();
				boolean expired = exp<now;
				boolean deceased = !e.getValue().isRunning();
				if (expired || deceased) {
					if (expired) {
						expcount++;
					}
					if (deceased) {
						deccount++;
					}
					i.remove();
				}
			}
			if (expcount>0 || deccount>0) {
				logger.info("Removed "+expcount+" expired and "+deccount+" deceased gateway session leases. Now holding "+this.sessions.size()+" sessions.");
			}
		} 
	}

	private final static Logger logger = Logger.getLogger(GatewaySessionManager.class.getName());

}
	