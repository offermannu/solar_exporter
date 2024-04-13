/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.management;

import java.lang.management.ManagementFactory;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.resources.util.ExpirableValuesPseudoMap;


/**
 * A convenient place to manage MBeans for resources that may be subject to garbage collection.
 * Any call to reregister will first unregister all Mbeans that depended on a collected resource.
 * 
 * Register an mbean specifying the dependency resource. 
 * 
 * @author hb
 *
 */
public class MBeanHub {
	
	private static class ObjectNameMap extends ExpirableValuesPseudoMap<Object,Object,ObjectName> {};
	
	private static ObjectNameMap map = new ObjectNameMap();
	private static Queue<ObjectNameMap.ValueHolder> queue = new LinkedBlockingQueue<ObjectNameMap.ValueHolder>();
	
	static {
		map.setInvalidationQueue(queue);
	}
	
	private static synchronized void tick() {
		ObjectNameMap.ValueHolder vh;
		map.tick();
		do {
			vh = queue.poll();
			if (vh!=null) {
				try {
					ObjectName name = vh.getExtra();
					MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
					if (mbs.isRegistered(name)) {
						mbs.unregisterMBean(name);
					}
				} catch (Exception e) {
					logger.log(Level.SEVERE,"Error during late unregistration of Mbean "+vh.getExtra(),e);
				}
			}
		} while (vh!=null);
	}
	
	/**
	 * Register an mbean specifying the dependency resource. If the dependency gets collected, eventually the mbean will be unregistered.
	 * If an mbean for the given name was registered before, it will be unregistered before the new mbean gets registered.
	 * 
	 * @param dependency
	 * @param mbean
	 * @param name
	 * @return
	 * @throws JMException
	 */
	public synchronized static ObjectInstance registerMBean(IResourceHandle dependency, Object mbean, ObjectName name) throws JMException{
		tick();
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			if (mbs.isRegistered(name)) {
				mbs.unregisterMBean(name);
			}
			return mbs.registerMBean(mbean, name);
		} finally {
			ObjectNameMap.ValueHolder vh = map.put(new Object(), dependency);
			vh.setExtra(name);
		}
	}

	private final static Logger logger = Logger.getLogger(MBeanHub.class.getName());
}
