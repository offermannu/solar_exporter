/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.management.home;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Set;

import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

/**
 * remotely available MBeanServer functions
 * 
 * @author hb
 * 
 */
public interface IHomeMBeanServer {

	Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException,
			ReflectionException, IOException, RemoteException;

	Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException;

	Integer getMBeanCount() throws IOException;

	Object getAttribute(ObjectName name, String attribute) throws IOException, MBeanException, ReflectionException, AttributeNotFoundException,
			InstanceNotFoundException;;

	AttributeList getAttributes(ObjectName name, String[] attributes) throws IOException, MBeanException, ReflectionException,
			AttributeNotFoundException, InstanceNotFoundException;

}
