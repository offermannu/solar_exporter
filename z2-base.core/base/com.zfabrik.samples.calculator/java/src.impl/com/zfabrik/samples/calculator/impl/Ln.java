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

public class Ln extends UnaryOp {

	private static final long serialVersionUID = 1L;

	@Override
	Elem compute(Elem... args) {
		try {
			return Const.fromDouble(Math.log(args[0].asDouble()));
		} catch (Exception e) {
			return new Error();
		}
	}

	@Override
	public String toString() {
		return "ln(x)";
	}
}
