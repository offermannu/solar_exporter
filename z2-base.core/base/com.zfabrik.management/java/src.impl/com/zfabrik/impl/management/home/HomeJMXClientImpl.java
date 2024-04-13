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

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import com.zfabrik.management.home.IHomeJMXClient;
import com.zfabrik.management.home.IHomeMBeanServer;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.workers.IMessageHandler;
import com.zfabrik.workers.home.IWorkerProcess;
import com.zfabrik.workers.home.WorkerUtils;
import com.zfabrik.workers.worker.HomeHandle;

public class HomeJMXClientImpl extends Resource implements IHomeJMXClient {
	public static final String CMD_INVOKE = "invoke";
	public static final String CMD_QUERY_NAMES = "queryNames";
	public static final String CMD_GET_MBEAN_COUNT = "getMBeanCount";
	public static final String CMD_GET_ATTRIBUTE = "getAttribute";
	public static final String CMD_GET_ATTRIBUTES = "getAttributes";
	public static final String JMX_CMDGROUP = "com.zfabrik.management.home";
	public static final String JMX_INVOKE_NAME = "invoke.name";
	public static final String JMX_INVOKE_OPERATION = "invoke.operation";
	public static final String JMX_INVOKE_PARAMS = "invoke.params";
	public static final String JMX_INVOKE_SIGNATURE = "invoke.signature";
	public static final String JMX_QUERY_NAME = "query.name";
	public static final String JMX_QUERY_QUERY = "query.query";
	public static final String JMX_GET_ATT_NAME = "attr.name";
	public static final String JMX_GET_ATT_ATTRIBUTE = "attr.attribute";

	private class RemoteMBeanServerImpl implements IHomeMBeanServer {
		private String workerName;
		private String workerComponent;

		public RemoteMBeanServerImpl(String workerName) {
			this.workerName = workerName;
			this.workerComponent = WorkerUtils.getWorkerComponentName(this.workerName);
		}

		private boolean isHere() {
			return (Foundation.isWorker() && this.workerName != null && 
					this.workerComponent.equals(Foundation.getProperties().get(Foundation.PROCESS_WORKER)))
					|| (!Foundation.isWorker() && this.workerName == null);
		}

		/*
		 * call remotely via cross worker/home communication
		 */
		private Object call(Map<String, Serializable> msg) throws RemoteException, IOException {
			if (this.workerName == null) {
				// target is home
				msg.put(IWorkerProcess.MSG_TARGET, IWorkerProcess.MSG_TARGET_HOME);
			} else {
				// target is worker
				msg.put(IWorkerProcess.MSG_NODE, this.workerName);
				msg.put(IWorkerProcess.MSG_TARGET, IWorkerProcess.MSG_TARGET_NODE);
			}
			msg.put(IMessageHandler.COMMAND_GROUP, JMX_CMDGROUP);
			Map<String, Serializable> res;
			if (Foundation.isWorker()) {
				// call worker via home
				HomeHandle hh = HomeHandle.instance();
				res = hh.sendMessage(msg, -1);
			} else {
				// call worker from home
				IWorkerProcess wp = WorkerUtils.getWorkerProcess(this.workerName);
				if (wp == null) {
					throw new IllegalStateException("Worker process (" + this.workerName+ ") not found");
				}
				msg.put(IMessageHandler.COMMAND_GROUP, JMX_CMDGROUP);
				res = wp.sendMessage(msg, -1);
			}
			return res.get(IMessageHandler.RETURN);
		}

		public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
				MBeanException, ReflectionException, IOException {
			if (this.isHere()) {
				return ManagementFactory.getPlatformMBeanServer().invoke(name, operationName, params, signature);
			} else {
				Map<String, Serializable> msg = new HashMap<String, Serializable>();
				msg.put(IMessageHandler.COMMAND, CMD_INVOKE);
				msg.put(JMX_INVOKE_NAME, name);
				msg.put(JMX_INVOKE_OPERATION, operationName);
				msg.put(JMX_INVOKE_PARAMS, params);
				msg.put(JMX_INVOKE_SIGNATURE, signature);
				return this.call(msg);
			}
		}

		@SuppressWarnings("unchecked")
		public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
			if (this.isHere()) {
				return ManagementFactory.getPlatformMBeanServer().queryNames(name, query);
			} else {
				Map<String, Serializable> msg = new HashMap<String, Serializable>();
				msg.put(IMessageHandler.COMMAND, CMD_QUERY_NAMES);
				msg.put(JMX_QUERY_NAME, name);
				msg.put(JMX_QUERY_QUERY, query);
				return (Set<ObjectName>) this.call(msg);
			}
		}

		public Integer getMBeanCount() throws IOException {
			if (this.isHere()) {
				return ManagementFactory.getPlatformMBeanServer().getMBeanCount();
			} else {
				Map<String, Serializable> msg = new HashMap<String, Serializable>();
				msg.put(IMessageHandler.COMMAND, CMD_GET_MBEAN_COUNT);
				return (Integer) this.call(msg);
			}
		}

		public Object getAttribute(ObjectName name, String attribute) throws IOException, MBeanException, ReflectionException,
				AttributeNotFoundException, InstanceNotFoundException {
			if (this.isHere()) {
				return ManagementFactory.getPlatformMBeanServer().getAttribute(name, attribute);
			} else {
				Map<String, Serializable> msg = new HashMap<String, Serializable>();
				msg.put(IMessageHandler.COMMAND, CMD_GET_ATTRIBUTE);
				msg.put(JMX_GET_ATT_NAME, name);
				msg.put(JMX_GET_ATT_ATTRIBUTE, attribute);
				return this.call(msg);
			}
		}

		@Override
		public AttributeList getAttributes(ObjectName name, String[] attributes) throws IOException, MBeanException, ReflectionException,
				AttributeNotFoundException, InstanceNotFoundException {
			if (this.isHere()) {
				return ManagementFactory.getPlatformMBeanServer().getAttributes(name, attributes);
			} else {
				Map<String, Serializable> msg = new HashMap<String, Serializable>();
				msg.put(IMessageHandler.COMMAND, CMD_GET_ATTRIBUTES);
				msg.put(JMX_GET_ATT_NAME, name);
				msg.put(JMX_GET_ATT_ATTRIBUTE, attributes);
				return (AttributeList) this.call(msg);
			}
		}

	}

	public <T> T as(Class<T> clz) {
		if (IHomeJMXClient.class.equals(clz)) {
			return clz.cast(this);
		}
		return null;
	}

	public IHomeMBeanServer getRemoteMBeanServer(String workerName) {
		return new RemoteMBeanServerImpl(workerName);
	}

}
