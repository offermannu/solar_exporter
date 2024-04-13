/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.naming.jndi.provider;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.ObjectFactory;
import javax.naming.spi.StateFactory;

/**
 * Programming model for naming integration. Allows custom provisioning
 * of Initial Context Factory, Object Factory, etc
 *
 * @author Henning Blohm
 */
public interface INamingProvider
{

	/**
	 * gets an Initial Context Factory.
	 *  
	 */
	public InitialContextFactory getInitialContextFactory(Hashtable<?,?> env) throws NamingException;
        
	/**
	 * gets an' URL Context Factory for the specified scheme and environment
	 * 
	 */
	public ObjectFactory getURLContextFactory(String scheme, Hashtable<?,?> env) throws NamingException;

	/**
	 * gets an object factory for the given name
	 * 
	 */
	public ObjectFactory getObjectFactory(String name) throws NamingException;

	/**
	 * returns a state factory for the 
	 * given name.
	 * 
	 * @param name name of the state factory
	 * @return a state factory 
	 * @throws NamingException
	 */
	public StateFactory getStateFactory(String name) throws NamingException;
        
}
