/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.servletjsp.security;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.LoginService;

import com.zfabrik.components.IComponentsManager;

/**
 * A convenience factory for z2 component resource based user realm implementations.
 * <p>
 * Declare as 
 * <pre>
 *   &lt;Call name="addBean"&gt;
 *     &lt;Arg&gt;
 *      &lt;Call class="com.zfabrik.servletjsp.security.ComponentHashLoginServiceFactory" name="loginService"&gt;
 *        &lt;Arg>realm&lt;/Arg&gt;
 *        &lt;Arg>environment/webUsers&lt;/Arg&gt;
 *      &lt;/Call&gt;
 *    &lt;/Arg&gt;
 *  &lt;/Call&gt;
 * </pre>
 * 
 * The component resource folder is expected to hold a file <code>realm.properties</code> that holds 
 * Jetty-style defined users. See also 
 * <a href="http://wiki.eclipse.org/Jetty/Tutorial/Realms">http://wiki.eclipse.org/Jetty/Tutorial/Realms</a>.
 *  
 * @author hb
 *
 */
public class ComponentHashLoginServiceFactory  {
	public static final String REALM_PROPERTIES = "realm.properties";
	
	public static LoginService getLoginService(String name, String componentName) throws IOException {
		File f = IComponentsManager.INSTANCE.retrieve(componentName);
		if (f==null) {
			throw new IllegalArgumentException("User realm component "+componentName+" not found");
		}
		f = new File(f,REALM_PROPERTIES);
		if (!f.exists()) {
			throw new IllegalArgumentException("User realm file "+f+" not found");
		}
		return new HashLoginService(name, f.getAbsolutePath());
	}
}
