/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidationException extends Exception {
	private static final long serialVersionUID = 1L;
	private List<PropertyValidationError> errors = new ArrayList<PropertyValidationError>();
	
	public ValidationException() {super();}
	
	public ValidationException(PropertyValidationError ...errors) {
		super();
		this.errors.addAll(Arrays.asList(errors));
	}
	
	public List<PropertyValidationError> getErrors() {
		return errors;
	}
	
	public boolean hasErrors() {
		return this.errors.size()>0;
	}
	
	public String toString() {
		if (errors.isEmpty()) {
			return "Validation Error (unknown cause)";
		}
		StringBuilder sb = new StringBuilder("Validation Error! Bad properties: ");
		for (PropertyValidationError pve : errors) {
			sb.append("\n").append(pve.toString());
		}
		return sb.toString();
	}
}
