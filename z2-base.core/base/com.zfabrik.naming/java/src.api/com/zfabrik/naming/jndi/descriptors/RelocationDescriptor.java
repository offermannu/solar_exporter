/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.naming.jndi.descriptors;

import javax.naming.Context;
import javax.naming.Name;

/**
 * A move-to descriptor serves to move or copy a named object to the binding location.
 * It serves to mediate move operations.
 * 
 * @author Henning
 *
 */
public class RelocationDescriptor
{
	public static final short MOVE = 0;
	public static final short COPY = 1;
	
	private Context sourceContext;
	private Name sourceName;
	private short type;
	
	public RelocationDescriptor(Context sourceContext, Name sourceName, short type)
	{
		this.sourceContext = sourceContext;
		this.sourceName = sourceName;
		this.type = type;
	}
	
	public Name getSourceName()
	{
		return this.sourceName;
	}
	
	public Context getSourceContext()
	{
		return this.sourceContext;
	}
	
	public short getType()
	{
		return this.type;
	}
}
