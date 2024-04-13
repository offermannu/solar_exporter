/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.servletjsp.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.WebAppContext;

public interface IWebServer {
	void start();
	void stop();
	
	void addHandler(Handler h);
	void removeHandler(Handler h);
	
	void configure(WebAppContext wac);
}
