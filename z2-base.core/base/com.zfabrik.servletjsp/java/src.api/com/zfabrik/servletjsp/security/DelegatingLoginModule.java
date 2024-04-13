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

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import com.zfabrik.components.IComponentsLookup;


/**
 * This login module implementation delegates to a login module component - usually of
 * component type <code>javax.security.auth.spi.LoginModule</code> - 
 * that provides the actual login module implementation as {@see LoginModule}. 
 * <p>
 * The purpose of this login module is to de-couple the jetty server
 * classpath from the login module implementation class path.
 * <p>
 * The login module component must be specified as configuration property
 * <code>com.zfabrik.security.componentName</code>.
 * <p>
 * A typical JAAS configuration using this login module looks like this:
 * <pre>
 * user_realm {
 *   com.zfabrik.servletjsp.security.DelegatingLoginModule sufficient
 *   com.zfabrik.security.componentName="test.loginModule/module"
 *   userName="willi"
 *   password="meier"
 *   roles="master,loser"
 *   ;
 * }; 
 * </pre>
 * 
 * @see {@link http://docs.codehaus.org/display/JETTY/JAAS}
 * @author hb
 * 
 *
 */
public class DelegatingLoginModule implements LoginModule {
	private LoginModule lm; 

	public DelegatingLoginModule() {}

	public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
		String componentName = (String) options.get("com.zfabrik.security.componentName");
		if (componentName==null) {
			throw new IllegalStateException("MUST specify componentName in LoginModule config!");
		}
		this.lm = IComponentsLookup.INSTANCE.lookup(componentName,LoginModule.class);
		if (this.lm==null) {
			throw new IllegalStateException("Failed to retrieve login module from component ("+componentName+")");
		}
		this.lm.initialize(subject, callbackHandler, sharedState, options);
	}

	public boolean abort() throws LoginException {
		return this.lm.abort();
	}

	public boolean commit() throws LoginException {
		return this.lm.commit();
	}

	public boolean login() throws LoginException {
		return this.lm.login();
	}

	public boolean logout() throws LoginException {
		return this.lm.logout();
	}
}
