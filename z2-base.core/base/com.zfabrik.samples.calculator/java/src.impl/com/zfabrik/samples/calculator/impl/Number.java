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

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.apache.commons.lang3.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

import java.util.Stack;


public class Number extends Elem {

	private static final long serialVersionUID = 1L;

	public static final Number Zero = new Number("0");

	private String number;
	
	protected int signum = 1;

	public Number(String number) {
		this.number = beautify(number);
	}

	public Number(double d) {
		this(String.valueOf(d));
	}

	private String beautify(String n) {
		n = trimToEmpty(n);
		
		if (isEmpty(n)) return "0";
		
		// remove leading '-'
		if (n.startsWith("-")) {
			switchSignum();
			n = substring(n, 1);
		} 
		
		// remove leading '0'
		n = stripStart(n,  "0");
		if (isEmpty(n) || startsWith(n, ".")) n = "0" + n;
				
		return n;
	}
	
	
	@Override
	int getPrio() {
		return 4;
	}

	@Override
	double asDouble() {
		return Double.parseDouble(number) * this.signum;
	}

	@Override
	public String toString() {
		return (asDouble() < 0? "-" : "") + this.number;
	}

	@Override
	boolean isNumber() {
		return true;
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
		return false;
	}

	Elem switchSignum() {
		this.signum *= -1;
		return this;
	}
	
	@Override
	void addTo(Stack<Elem> stack) {
		if (stack.peek().isConst()) {
			// replace number with const
			stack.pop();
			stack.push(this);
			
		} else if (stack.peek().isNumber()) {
			// extend number
			stack.push(new Number(stack.pop().toString() + this.number));
			
		} else {
			// start new number
			stack.push(this);
		}
	}

	@Override
	boolean solve(Stack<Elem> stack) {
		// number.solve r 	-> r :: [number], false
		stack.push(this);
		return false;
	}
	
	@Override
	Elem compute(Elem... args) {
		// remove leading and trailing '0'
		this.number = String.valueOf(asDouble());
		return this;
	}
	
}