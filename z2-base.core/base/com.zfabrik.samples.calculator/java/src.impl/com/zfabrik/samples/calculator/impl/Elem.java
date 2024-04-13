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

import java.io.Serializable;
import java.util.Stack;

public abstract class Elem implements Serializable {

	private static final long serialVersionUID = 1L;

	abstract boolean isNumber();

	abstract boolean isConst();

	abstract boolean isOp();

	abstract boolean isError();

	abstract boolean isOpenPar();

	abstract double asDouble();
	
	abstract int getPrio();
	
	Elem switchSignum() {
		throw new UnsupportedOperationException();
	}
	
	/**
	 * addTo must ensure stack sanity (i.e. a valid order of nums and ops)
	 */
	abstract void addTo(Stack<Elem> stack);
	
	/**
	 * Performs a single resolve step.
	 * Returns false, if definitely another resolve would leave the stack unchanged, true otherwise.
	 *  
	 * @param stack
	 * @return true, if solving should be continued, false otherwise
	 */
	abstract boolean solve(Stack<Elem> stack);
	
	abstract Elem compute(Elem... args);
	
	String toHTML() {
		return toString();
	}
}
