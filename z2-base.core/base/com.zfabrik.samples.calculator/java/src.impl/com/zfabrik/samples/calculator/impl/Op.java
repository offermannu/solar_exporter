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

public abstract class Op extends Elem {

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
		return true;
	}

	@Override
	boolean isError() {
		return false;
	}

	@Override
	boolean isOpenPar() {
		return false;
	}
	
	@Override
	double asDouble() {
		return Double.NaN;
	}

	@Override
	void addTo(Stack<Elem> stack) {
		// op.addTo r :: [op2]	-> r :: [op]
		// op.addTo r :: [z]	-> r :: [z, op]	-- ensures that operations are always preceded by a number 
		// op.addTo r :: [open] -> r :: [open]	-- don't replace '(' because '(' could be preceded by another operation
		if (stack.peek().isOp()) {
			// replace op
			stack.pop();
		} 
		
		if (! stack.peek().isOpenPar()) {
			stack.push(this);
		}
	}
}
