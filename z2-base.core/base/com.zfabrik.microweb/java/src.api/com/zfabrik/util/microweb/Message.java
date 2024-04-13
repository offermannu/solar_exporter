/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.microweb;

public class Message {
	private final static String[] STRINGS  = new String[]{"Info: ","Error: "};
	
	public final static short INFO = 0;
	public final static short ERROR = 1;

	private String message;
	private short type;

	public Message(short type, String message) {
		this.message = message;
		this.type = type;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public short getType() {
		return type;
	}

	public void setType(short type) {
		this.type = type;
	}

	public String toString() {
		return STRINGS[type] + message;
	}

}
