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

public class ClosePar extends Elem {

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
	boolean isError() {
		return false;
	}

	@Override
	boolean isOpenPar() {
		return true;
	}

	@Override
	double asDouble() {
		return Double.NaN;
	}

	@Override
	int getPrio() {
		return 1;
	}
	
	@Override
	void addTo(Stack<Elem> stack) {
		// allow ) only after numbers
		if (stack.peek().isNumber()) {
			stack.push(this);
		}
	}

	@Override
	boolean solve(Stack<Elem> stack) {
		// stack top must be a number (see #addTo())
		// close.solve [z] -> [z], false
		// close.solve r :: [open, z] -> [z], false
		// close.solve r :: [z1, op, z2] -> r :: [z1 op z2, close], true	-- operations are always preceded by numbers
		
		if (stack.size() == 1) {
			return false;
		}
		
		Elem c = stack.pop();
		Elem b = stack.pop();
			
		if (b.isOpenPar()) {
			stack.push(c);
			return false;
		}
		
		// |stack| >= 3 && b must be op
		Elem a = stack.pop();
		stack.push(b.compute(a, c));
		stack.push(this);
		
		return true;
	}

	@Override
	Elem compute(Elem... args) {
		return new Error();
	}
	
	@Override
	public String toString() {
		return ")";
	}

}
