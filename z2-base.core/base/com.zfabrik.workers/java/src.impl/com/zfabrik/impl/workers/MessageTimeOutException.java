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


/**
 * @author hb
 *
 */
public class MessageTimeOutException extends IOException
{
    /**
     * 
     */
    private static final long serialVersionUID = 6174939920998217267L;

    public MessageTimeOutException(long ticket, long timeout)
    {
        super("Message for ticket "+ticket+" has expired (after "+timeout+"ms).");
    }
}
