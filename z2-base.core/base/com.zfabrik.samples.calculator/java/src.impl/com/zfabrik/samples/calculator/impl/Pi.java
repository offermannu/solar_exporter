/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.samples.calculator.impl;

public class Pi extends Const {

	private static final long serialVersionUID = 1L;

	public Pi() {
		super("pi");
	}

	@Override
	double asDouble() {
		return this.signum * Math.PI;
	}
	
	@Override
	String toHTML() {
		return (this.signum == -1? "-" : "") + "&pi;";
	}
}
