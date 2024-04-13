/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.management.home;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;
import javax.management.QueryExp;

import com.zfabrik.workers.IMessageHandler;

/**
 * supplying remote access to a local MBean Server
 * @author hb
 *
 */
public class JMXMessageHandler implements IMessageHandler {

	public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
		Map<String, Serializable> res = new HashMap<String, Serializable>();
		String cmd = (String) args.get(IMessageHandler.COMMAND);
		if (HomeJMXClientImpl.CMD_INVOKE.equals(cmd)) {
			res.put(IMessageHandler.RETURN,
					(Serializable) ManagementFactory.getPlatformMBeanServer().invoke(
							(ObjectName) args.get(HomeJMXClientImpl.JMX_INVOKE_NAME),
							(String) args.get(HomeJMXClientImpl.JMX_INVOKE_OPERATION),
							(Object[]) args.get(HomeJMXClientImpl.JMX_INVOKE_PARAMS),
							(String[]) args.get(HomeJMXClientImpl.JMX_INVOKE_SIGNATURE)
					)
			);
		} else
		if (HomeJMXClientImpl.CMD_QUERY_NAMES.equals(cmd)) {
			res.put(IMessageHandler.RETURN,
					(Serializable) ManagementFactory.getPlatformMBeanServer().queryNames(
							(ObjectName) args.get(HomeJMXClientImpl.JMX_QUERY_NAME),
							(QueryExp) args.get(HomeJMXClientImpl.JMX_QUERY_QUERY)
					)
			);
		} else 
		if (HomeJMXClientImpl.CMD_GET_MBEAN_COUNT.equals(cmd)) {
			res.put(IMessageHandler.RETURN,ManagementFactory.getPlatformMBeanServer().getMBeanCount()	);
		} else 
		if (HomeJMXClientImpl.CMD_GET_ATTRIBUTE.equals(cmd)) {
			res.put(IMessageHandler.RETURN,
					(Serializable) ManagementFactory.getPlatformMBeanServer().getAttribute(
							(ObjectName) args.get(HomeJMXClientImpl.JMX_GET_ATT_NAME),
							(String) args.get(HomeJMXClientImpl.JMX_GET_ATT_ATTRIBUTE)
					)
			);
		} else 
			if (HomeJMXClientImpl.CMD_GET_ATTRIBUTES.equals(cmd)) {
				res.put(IMessageHandler.RETURN,
						(Serializable) ManagementFactory.getPlatformMBeanServer().getAttributes(
								(ObjectName) args.get(HomeJMXClientImpl.JMX_GET_ATT_NAME),
								(String[]) args.get(HomeJMXClientImpl.JMX_GET_ATT_ATTRIBUTE)
						)
				);
		} else { 
			throw new IllegalArgumentException("Cannot handle command ("+cmd+")");
		}
		return res;
	}

}
