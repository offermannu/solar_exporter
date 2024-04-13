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

public abstract class BinaryOp extends Op {

	private static final long serialVersionUID = 1L;

	@Override
	boolean solve(Stack<Elem> stack) {
		// operations are always preceded by numbers
		// op.solve [z] -> [z, op], false
		// op.solve r :: [(, z1] -> r :: [(, z1, op], false
		// op.solve r :: [z1, op2, z2] 
		// 		if prio(op2) >= prio(op) -> r :: [z1 op2 z2, op], true
		// 		if prio(op2) < prio(op)  -> r :: [z1, op2, z2, op], false
		
		if (stack.size() == 1) {
			stack.push(this);
			return false;
		}
		
		Elem c = stack.pop();
		Elem b = stack.pop();
		
		if (b.isOpenPar()) {
			stack.push(b);
			stack.push(c);
			stack.push(this);
			
			return false;
		}
		
		// |stack| >= 3 && b must be op
		Elem a = stack.pop();

		// if b's prio >= this prio then b can be computed 
		if (b.getPrio() >= this.getPrio()) {
			stack.push(b.compute(a, c));
			if (! stack.peek().isError()) stack.push(this);
			return true;
				
		} else {
			stack.push(a);
			stack.push(b);
			stack.push(c);
			stack.push(this);
			return false;
		}
	}
}
