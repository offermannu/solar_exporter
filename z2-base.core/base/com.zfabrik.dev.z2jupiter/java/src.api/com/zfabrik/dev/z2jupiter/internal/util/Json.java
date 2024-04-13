/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.util;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Json {
	
	public static ObjectMapper om() {
		return new ObjectMapper()
			// pretty print
			.enable(SerializationFeature.INDENT_OUTPUT)
			// only non-null to be marshalled
			.setSerializationInclusion(Include.NON_NULL);
	}

	public static String toJson(Object value) {
		try {
			return om().writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T fromJson(String in, Class<T> clz) {
		try {
			return om().readValue(in,clz);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
