/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.z2unit;

/**
 * If a remote error could not be provided locally (typically due to serialization or
 * de-serialization problems, we use this local exception to provide some throwable
 * to JUnit. This exception wraps a textual representation of the remote error.
 */
public class RemoteErrorException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private String remoteStackTrace;
	
	public RemoteErrorException(String remoteStackTrace) {
		this.remoteStackTrace = remoteStackTrace;
	}
	
	public String getRemoteStackTrace() {
		return remoteStackTrace;
	}
	
	@Override
	public String getMessage() {
		return "This is a placeholder exception with the remote original stacktrace as message."+
			   "The original exception could not be provided, probably due to de-/serialization problems (see logs):"+
				remoteStackTrace;
	}
}
