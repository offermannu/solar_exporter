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

import java.util.regex.Pattern;

/**
 * Very simple email validator
 * @author hb
 *
 */
public class EmailValidator {
	private final static Pattern PT_EMAIL = Pattern.compile("^([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9._%+-]+)$"); 
	
	public static boolean isValid(String email) {
		return PT_EMAIL.matcher(email).matches();
	}

}

