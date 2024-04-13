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

public class CE extends Op {

	private static final long serialVersionUID = 1L;

	@Override
	int getPrio() {
		return Integer.MAX_VALUE;
	}
	
	@Override
	void addTo(Stack<Elem> stack) {
		stack.add(this);
	}
	
	@Override
	boolean solve(Stack<Elem> stack) {
		stack.clear();
		stack.add(Number.Zero);
		return false;
	}

	@Override
	Elem compute(Elem... args) {
		return new Error();
	}
	
	@Override
	public String toString() {
		return "CE";
	}

}
