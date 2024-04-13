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

public class MessageException extends IOException {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7670789922635108205L;

	public MessageException(String s)   {
        super(s);
    }

    public MessageException(String s,Throwable t)   {
        super(s);
        super.initCause(t);
    }

}
