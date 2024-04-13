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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.lang3.StringUtils;

import com.zfabrik.util.html.Escaper;

public class Calculator {

	private final static Logger logger = Logger.getLogger(Calculator.class.getName());
	
	private enum OP {
		CE, C, open, close, 
//		sqrt, pow, ln, epowx,
		sgn, dot, eq, pi, 
		add, sub, mult, div, 
		sqr, inv, illegal
	};

	private Stack<Elem> stack = new Stack<Elem>();
	private String op;
	private String num;

	@SuppressWarnings("unchecked")
	public void setParams(Map<String, String> params) {

		String stackVal = params.get("serStack");
		if (! StringUtils.isEmpty(stackVal)) {
			try {
				ObjectInputStream ois = new ObjectInputStream(new Base64InputStream(new ByteArrayInputStream(stackVal.getBytes()), false));
				this.stack = (Stack<Elem>) ois.readObject();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Failed to decode stack", e);
			}
		}

		if (this.stack.isEmpty()) {
			this.stack.push(Number.Zero);
		}

		this.num = params.get("num");

		// the op-field has the syntax "op=xyz", because IE7 does not submit the value of a button
		for (String k : params.keySet()) {
			if (k.startsWith("op=")) {
				this.op = k.split("=")[1];
			}
		}
	}

	public String getResult() {

		if (this.op != null) {
			doOp(getOp());

		} else if (this.num != null) {
			doNum(this.num);
		}

		// resolve the stack: rest :: [top] -> top.solve(rest)
		while (this.stack.pop().solve(this.stack));

		return printStack();
	}

	private OP getOp() {
		try {
			return OP.valueOf(this.op);
		} catch (Exception e) {
			// this.op either illegal or null
			return OP.illegal;
		}
	}

	public void doNum(String num) {
		if (! isError()) {
			new Number(num).addTo(this.stack);
		}
	}

	public void doOp(OP op) {
		
		// only CE and C are allowed in error case
		switch (op) {
		case CE:
			new CE().addTo(this.stack);
			return;

		case C:
			new C().addTo(this.stack);
			return;
		}

		if (! isError()) {
			switch (op) {
			case open:
				new OpenPar().addTo(this.stack);
				break;

			case close:
				new ClosePar().addTo(this.stack);
				break;

			case sgn:
				new Sgn().addTo(this.stack);
				break;

			case dot:
				new Dot().addTo(this.stack);
				break;

			case eq:
				new Eq().addTo(this.stack);
				break;

			case pi:
				new Pi().addTo(this.stack);
				break;

			case add:
				new Add().addTo(this.stack);
				break;

			case sub:
				new Sub().addTo(this.stack);
				break;

			case mult:
				new Mult().addTo(this.stack);
				break;

			case div:
				new Div().addTo(this.stack);
				break;

			case sqr:
				new Sqr().addTo(this.stack);
				break;

			case inv:
				new Inv().addTo(this.stack);
				break;
/* ---				
			case sqrt:
				new Sqrt().addTo(this.stack);
				break;
				
			case pow:
				new Pow().addTo(this.stack);
				break;

			case ln:
				new Ln().addTo(this.stack);
				break;
				
			case epowx:
				new EPowX().addTo(this.stack);
				break;
 */
				
			default:
				new Error().addTo(this.stack);
			}
		}
	}

	/**
	 * @return human readable version
	 */
	public String printStack() {
		StringBuilder result = new StringBuilder();
		
		for (Elem e : this.stack) {
			result.append(e.toHTML()).append(' ');
		}
		return result.toString();
	}
	
	/**
	 * @return serialized version
	 */
	public String getSerStack() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(new Base64OutputStream(baos));
			oos.writeObject(this.stack);
			oos.close();
			return Escaper.escapeToHTML(baos.toString());
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Failed to encode stack", e);
			return "";
		}
	}
	
	public Stack<Elem> getStack() {
		return stack;
	}

	private boolean isError() {
		return this.stack.peek().isError();
	}
	
	public static void main(String[] args) {
		System.out.println(OP.valueOf(null));
	}

}
