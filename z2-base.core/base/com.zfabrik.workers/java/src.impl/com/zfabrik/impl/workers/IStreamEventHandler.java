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
public interface IStreamEventHandler
{
    public void process(String line)
    	throws IOException;
    public void close();
}
