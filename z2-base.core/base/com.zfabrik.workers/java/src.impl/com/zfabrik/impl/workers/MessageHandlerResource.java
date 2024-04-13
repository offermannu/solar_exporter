/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.expression.X;
import com.zfabrik.workers.IMessageHandler;

public class MessageHandlerResource extends Resource {
	private String name;
	private MH mh;

	private class MH implements IMessageHandler {
		private IMessageHandler mh;

		public MH(IMessageHandler mh) {
			this.mh = mh;
		}

		public Map<String, Serializable> processMessage(Map<String, Serializable> args) throws Exception {
			return this.mh.processMessage(args);
		}
	}

	public MessageHandlerResource(String name) {
		this.name = name;
	}

	public <T> T as(Class<T> clz) {
		synchronized (this) {
			if (IMessageHandler.class.equals(clz)) {
				if (mh == null) {
					this.mh = new MH(JavaComponentUtil.loadImplementationFromJavaComponent(this.name, IComponentDescriptor.COMPONENT_CLZ, handle(), IMessageHandler.class));
				}
			}
		}
		return clz.cast(mh);
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		this.mh = null;
	}

	// --------------- helper ------------------
	public static  Map<String, Serializable> forwardMessageToHandler(Logger logger, Map<String, Serializable> args)
			throws IOException, Exception {
		String group= (String) args.get(IMessageHandler.COMMAND_GROUP);
		if (group==null) 
			throw new IllegalArgumentException("No command group specified in message args");
		// find the message handler
		Collection<String> mhs = IComponentsManager.INSTANCE.findComponents(X.and(X.var(IComponentDescriptor.COMPONENT_TYPE).eq(
				X.val(IMessageHandler.TYPE)), X.var(IMessageHandler.COMMAND_GROUP).eq(X.val(group))));
		if (mhs.size() == 0)
			throw new IllegalStateException("Failed to retrieve message handler for command group (" + group + ")");
		if (mhs.size() > 1)
			throw new IllegalStateException("Found more than one message handler for command group (" + group + ")");
		String n = mhs.iterator().next();
		IMessageHandler mh = IComponentsLookup.INSTANCE.lookup(n, IMessageHandler.class);
		if (mh == null)
			throw new IllegalStateException("Failed to retrieve message handler (" + n + ") for command group (" + group + ")");
		logger.finer("Calling Message Handler:" + n);
		return mh.processMessage(args);
	}

}
