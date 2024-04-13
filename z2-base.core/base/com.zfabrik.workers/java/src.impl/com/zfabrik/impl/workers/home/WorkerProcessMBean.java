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

import java.util.Date;

public interface WorkerProcessMBean {
	String getComponentName();
	String getName();
	String getPID();
	int  getNumberAttemptedStarts();
	int  getNumberSuccesfulStarts();
	Throwable getLastError();
	Date getLastSuccessfulStart();
	Date getLastAttemptedStart();
	Date getDetachTime();
	short  getState();
	
	void detach();
	void stop();
	void restart();
	long ping();

}
