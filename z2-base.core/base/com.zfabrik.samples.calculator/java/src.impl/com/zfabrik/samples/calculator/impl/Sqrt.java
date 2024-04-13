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

public class Sqrt extends UnaryOp {

	private static final long serialVersionUID = 1L;

	@Override
	Elem compute(Elem... args) {
		try {
			double x = args[0].asDouble();
			return Const.fromDouble(Math.sqrt(x));
		} catch (Exception e) {
			return new Error();
		}
	}

	@Override
	public String toString() {
		return "sqrt(x)";
	}
}
