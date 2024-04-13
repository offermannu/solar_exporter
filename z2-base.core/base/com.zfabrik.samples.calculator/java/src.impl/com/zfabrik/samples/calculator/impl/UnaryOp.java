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

public abstract class UnaryOp extends Op {

	private static final long serialVersionUID = 1L;

	@Override
	int getPrio() {
		return Integer.MAX_VALUE;
	}

	@Override
	boolean solve(Stack<Elem> stack) {
		// operations are always preceded by numbers, and unary operations have top prio, so they can be directly executed
		stack.push(compute(stack.pop()));
		return false;
	}

}
