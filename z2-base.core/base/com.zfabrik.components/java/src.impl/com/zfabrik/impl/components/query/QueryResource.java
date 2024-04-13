/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.components.query;

import java.io.IOException;
import java.util.Collection;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.query.IComponentQuery;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.expression.X;

public class QueryResource extends Resource implements IComponentQuery {
	private X x;
	private long crc;
	
	public QueryResource(String name) {}

	public synchronized <T> T as(Class<T> clz) {
		if (clz.equals(IComponentQuery.class)) {
			return clz.cast(this);
		}
		return null;
	}
	
	public synchronized void invalidate() throws ResourceBusyException {
		this.x = null;
	}
	
	public synchronized void setX(X x) {
		this.x = x;
		// compute check sum out of query result and keep it
		this.crc = this._checkSum(x);
	}

	private long _checkSum(X q) {
		try {
			Collection<String> cs = IComponentsManager.INSTANCE.findComponents(q);
			long s = 0;
			for (String cn : cs) {
				IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(cn);
				// rotate left
				if (s<0) {
					s = (s<<1) + 1;
				} else {
					s = s << 1;
				}
				// xor with revision
				s = (s ^ d.getRevision());
			}
			return s;
		} catch (IOException e) {
			throw new RuntimeException("Query check sum computation failed: "+q,e);
		}
	}

	public synchronized X getX() {
		return this.x;
	}
	
	public synchronized boolean verify() {
		return (this.x==null) || (this.crc == this._checkSum(this.x));
	}
	
}
