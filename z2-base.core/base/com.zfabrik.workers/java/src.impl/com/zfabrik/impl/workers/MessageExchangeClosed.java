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

public class MessageExchangeClosed extends MessageException {
	private static final long serialVersionUID = 1L;

	public MessageExchangeClosed() {
		super("Message exchange closed");
	}
}
