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

public class Error extends Elem {

	private static final long serialVersionUID = 1L;

	@Override
	boolean isNumber() {
		return false;
	}

	@Override
	boolean isConst() {
		return false;
	}
	
	@Override
	boolean isOp() {
		return false;
	}
	
	@Override
	boolean isOpenPar() {
		return false;
	}
	
	@Override
	boolean isError() {
		return true;
	}

	@Override
	int getPrio() {
		return Integer.MAX_VALUE;
	}
	
	@Override
	double asDouble() {
		return Double.NaN;
	}
	
	@Override
	public String toString() {
		return "Err";
	}

	@Override
	void addTo(Stack<Elem> stack) {
		// error matches everywhere
		stack.push(this);
	}
	
	@Override
	boolean solve(Stack<Elem> stack) {
		// error.solve r -> r :: [error], false
		stack.push(this);
		return false;
	}
	
	@Override
	Elem compute(Elem... args) {
		return this;
	}
}
