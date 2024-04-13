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

public class OpenPar extends Elem {

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
		// open.addTo [0]         -> [open]				-- replace 0 
		// open.addTo r :: [open] -> r :: [open, open]
		// open.addTo r :: [op]   -> r :: [op, open]
		// open.addTo r :: [z]    -> r :: [z]			-- ignore open
		
		if (stack.size() == 1 && stack.peek().asDouble() == 0.0d) {
			stack.pop();
			stack.push(this);
		} else if (stack.peek().isOp() || stack.peek().isOpenPar()) {
			stack.push(this);
		}
	}

	@Override
	boolean solve(Stack<Elem> stack) {
		// open.solve r -> r :: [open], false 
		stack.push(this);
		return false;
	}

	@Override
	Elem compute(Elem... args) {
		return new Error();
	}

	@Override
	public String toString() {
		return "(";
	}
}
