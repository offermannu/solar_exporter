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

public class PropertyValidationError extends Exception {
	private static final long serialVersionUID = 1L;
	private String propertyName;
	private String propertyError;

	public PropertyValidationError(String propertyName, String propertyError) {
		this.propertyName = propertyName;
		this.propertyError = propertyError;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public String getPropertyError() {
		return propertyError;
	}

	public void setPropertyError(String propertyError) {
		this.propertyError = propertyError;
	}

	public String toString() {
		return propertyName + " (" + propertyError + ")";
	}
	
	public String getMessage() {
		return toString();
	}

	// TODO: add localized messages
}
