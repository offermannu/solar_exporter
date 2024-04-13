/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.gateway.home;

/**
 * The gateway handler maintains a map of session id to handling worker process.
 * Every entry is tagged by an expiration
 */
public class GatewaySessionId {
	private long expiration;
	private String id;
	
	public GatewaySessionId(String id, long expiration) {
		this.id = id;
		this.expiration = expiration;
	}
	
	public void setExpiration(long expiration) {
		this.expiration = expiration;
	}

	public long getExpiration() {
		return expiration;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	@Override
	public boolean equals(Object obj) {
		if (obj==this) {
			return true;
		}
		if (obj instanceof GatewaySessionId) {
			GatewaySessionId that = (GatewaySessionId) obj;
			return that.id.equals(this.id);
		}
		return false;
	}
	
	@Override
	public String toString() {
		return id;
	}
}