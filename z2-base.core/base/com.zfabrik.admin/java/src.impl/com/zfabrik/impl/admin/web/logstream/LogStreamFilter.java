package com.zfabrik.impl.admin.web.logstream;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;


import com.zfabrik.impl.admin.util.LogAccess;
import com.zfabrik.impl.admin.util.LogAccess.LogLines;

/**
 * A log stream provider filter that simply keeps streaming the log until the 
 * request is forcefully closed.
 */
public class LogStreamFilter implements Filter {
	private final static Logger LOG = Logger.getLogger(LogStreamFilter.class.getName());

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();
		try {
			LogAccess logAccess = new LogAccess();
			long base= logAccess.initializeLogStreaming();
			if (base<0) {
				out.println("Log Streaming is not available as the Log Buffer Handler is not registered.");
			} else {
				LOG.info("Started log stream for request from "+request.getRemoteAddr());
				while (!out.checkError()) {
					LogLines l = logAccess.fetchLines(base);
					String[] lines = l.getLines();
					Long actualBase = l.getFirstLineLogIndex();
		
		            if (actualBase!=base) {
		                out.println("WARNING: Missed "+(actualBase-base)+" lines"); 
		            }
		            for (String line : lines) {
		            	out.print(line);
		            }
		            base = actualBase+lines.length;
		            out.flush();
		            Thread.sleep(500);
				}
				LOG.info("Stopped log stream for request from "+request.getRemoteAddr());
			}
		} catch (IOException | ServletException e) {
			throw e;
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	@Override
	public void destroy() {}

}
