/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.web.vm;

import java.io.PrintWriter;

import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.impl.admin.util.LogAccess;
import com.zfabrik.impl.admin.util.LogAccess.LogLines;
import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.util.html.Escaper;
import com.zfabrik.util.microweb.actions.OutCome;

/**
 * Log streaming action bean
 */
public class Log {
	private HttpServletResponse response;
	private long base;
	private LogAccess logAccess = new LogAccess();
	
	public void setBase(long base) {
		this.base = base;
	}
	
	public long getBase() {
		return base;
	}
	
	public String getTitle() {
		return "log streamer";
	}
	
	public OutCome init(HttpServletRequest request,HttpServletResponse response) {
		this.response = response;
		return null;
	}
	
	// do show log
	public OutCome doByDefault() throws Exception {
		// get the base line
		this.base=logAccess.initializeLogStreaming();
		return OutCome.forward("/WEB-INF/log.jsp");
	}

	// do sync
	public OutCome doSync() throws Exception {
		// get the base line
		IHomeJMXClient hcl = IHomeJMXClient.INSTANCE;
		IHomeMBeanServer mbs = hcl.getRemoteMBeanServer(null);
		ObjectName on = ObjectName.getInstance("zfabrik:type=launchers.HomeLauncher");
		mbs.invoke(on, "synchronize", null, null);
		return doByDefault();
	}
	
	// do show log
	public OutCome doGetLines() throws Exception {
		LogLines l = logAccess.fetchLines(base);
		String[] lines = l.getLines();
		Long actualBase = l.getFirstLineLogIndex();
		// return lines as JSON
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.print("{\n\"actualBase\":");
		out.print(actualBase);
		out.print(",\n\"lines\":[\n");
		boolean first = true;
		for (String line : lines) {
			if (first) {
				first = false;
			} else {
				out.print(",\n");
			}
			out.print("\"");
			out.print(Escaper.escapeToJSON(line));
			out.print("\"");
		}
		out.print("\n]\n}\n");
		return OutCome.done();
	}

	

}
