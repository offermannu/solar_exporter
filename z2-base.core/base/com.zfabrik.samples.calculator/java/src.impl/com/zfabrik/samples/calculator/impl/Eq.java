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

public class Eq extends Op {

	private static final long serialVersionUID = 1L;

	@Override
	int getPrio() {
		return 0;
	}

	@Override
	void addTo(Stack<Elem> stack) {
		// remove all non-numbers from the top, so that the equation can be solved
		while (! (stack.isEmpty() || stack.peek().isNumber())) {
			stack.pop();
		}
		stack.push(this);
	}
	
	@Override
	boolean solve(Stack<Elem> stack) {
		// resolve the whole stack. unmatching open-pars are ignored
		// stack is either empty or stack-top is a number (see #addTo())
		// eq.solve [] 					-> [0], false					-- this happens when equation starts with (
		// eq.solve [z] 				-> [z], false					
		// eq.solve r :: [open, z] 		-> r :: [z, eq], true			-- resolve ( z ) to z 
		// eq.solve r :: [z1, op, z2] 	-> r :: [z1 op z2, eq], true 
		
		if (stack.isEmpty()) {
			stack.push(Number.Zero);
			return false;
		}
		
		if (stack.size() == 1) {
			stack.peek().compute();
			return false;
		}
		
		Elem c = stack.pop();
		Elem b = stack.pop();
		
		if (b.isOpenPar()) {
			stack.push(c);
			stack.push(this);
			
			return true;
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
		return "=";
	}

}
