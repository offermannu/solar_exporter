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

import java.util.Stack;

public class Const extends Number {

	private static final long serialVersionUID = 1L;
	
	public Const(String number) {
		super(number);
	}
	
	private Const(double d) {
		super(d);
	}
	
	public static Elem fromDouble(double d) {
		if (Double.isInfinite(d) || Double.isNaN(d)) {
			return new Error();
		} else {
			return new Const(d);
		}
	}
	@Override
	boolean isConst() {
		return true;
	}

	@Override
	void addTo(Stack<Elem> stack) {

		if (stack.peek().isNumber()) {
			// replace number
			stack.pop();
		}
		stack.push(this);
	}
}