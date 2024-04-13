/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.util;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.servlet.ServletException;

import com.zfabrik.impl.admin.web.logstream.LogStreamFilter;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;

/**
 * Utility for log access via home mbean
 */
public class LogAccess {
	private static final int bufferSize = 1000;
	private static final int expiresIn = 10000;
	private static final int chunkSize = 1000;

	/**
	 * Result type of {@link #getLines()}
	 */
	public static class LogLines {
		String[] lines;
		long firstLineLogIndex;
		
		public LogLines(String[] lines, long firstLineLogIndex) {
			super();
			this.lines = lines;
			this.firstLineLogIndex = firstLineLogIndex;
		}

		public String[] getLines() {
			return lines;
		}
		
		public long getFirstLineLogIndex() {
			return firstLineLogIndex;
		}
		
	}
	

	/**
	 * Method used her and in continuous streaming (see {@link LogStreamFilter}) to initiate log streaming.
	 * Returns current log base to start from
	 */
	public long initializeLogStreaming() throws Exception {
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
		CompositeData logBufferInfo = (CompositeData) mbs.getAttribute(on, "LogBufferInfo");
		if (logBufferInfo==null) {
			return -1;
//			throw new ServletException("Log Buffer Handler not registered!");
		}
		// set the base, so that we read from the buffer right away
		Long linesHandled = (Long) logBufferInfo.get("linesHandled");
		Integer currentSize = (Integer) logBufferInfo.get("size");
		return linesHandled - Math.min(bufferSize, currentSize);
	}
	
	/**
	 * Method used her and in continuous streaming (see {@link LogStreamFilter}) to fetch lines
	 * from the log buffer. 
	 */
	public LogLines fetchLines(long base) throws Exception {
		// get the base line
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");

		// get lines
		CompositeData logLines = (CompositeData) mbs.invoke(
			on, 
			"getLogBufferLines", 
			new Object[] { base, chunkSize }, 
			new String[] { "long","int" }
		);
		
		// reconfigure buffer
		mbs.invoke(
			on, 
			"configureLogBuffer", 
			new Object[] { bufferSize, System.currentTimeMillis()+expiresIn, true }, 
			new String[] {"int","long","boolean"}
		);
		
		return new LogLines(
			(String[]) logLines.get("lines"), 
			(Long) logLines.get("firstLineLogIndex")
		);
	}

}
