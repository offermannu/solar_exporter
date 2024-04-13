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


public class Dot extends UnaryOp {

	private static final long serialVersionUID = 1L;
	
	@Override
	Elem compute(Elem... args) {
		String x = args[0].toString();
		if (x.indexOf('.') == -1) {
			return new Number(x + '.');
		} else {
			return args[0];
		}
	}
	
	@Override
	public String toString() {
		return ".";
	}
}
