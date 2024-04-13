/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.mvncr;

import com.zfabrik.components.provider.util.FSComponentRepositoryDB;

public class MvnRepositoryDB extends FSComponentRepositoryDB {
	
	private static final long serialVersionUID = 1L;
	
	private byte[] settingsHash;

	public byte[] getSettingsHash() {
		return settingsHash;
	}
	
	public void setSettingsHash(byte[] settingsHash) {
		this.settingsHash = settingsHash;
	}
	
}
