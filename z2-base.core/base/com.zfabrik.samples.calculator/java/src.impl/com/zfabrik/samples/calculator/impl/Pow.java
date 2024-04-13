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

public class Pow extends BinaryOp {

	private static final long serialVersionUID = 1L;

	@Override
	int getPrio() {
		return 4;
	}

	@Override
	Elem compute(Elem... args) {
		try {
			return Const.fromDouble(Math.pow(args[0].asDouble(), args[1].asDouble()));
		} catch (Exception e) {
			return new Error();
		}
	}

	@Override
	public String toString() {
		return "x^y";
	}
	
	@Override
	String toHTML() {
		return "^";
	}
}
