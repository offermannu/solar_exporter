/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.naming.resources;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

public class RMInitialContextFactory implements InitialContextFactory {

	public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
		return new RMRootContext(environment);
	}

}
