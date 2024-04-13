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

public class Sgn extends UnaryOp {

	private static final long serialVersionUID = 1L;

	@Override
	Elem compute(Elem... args) {
		return args[0].switchSignum();
	}

	@Override
	public String toString() {
		return "+/-";
	}

	@Override
	String toHTML() {
		return "&plusmn;";
	}
}
