/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;

public class StringUtils {

	public final static boolean isEmpty(CharSequence s) {
		return (s == null) || (s.length() == 0);
	}

	public final static boolean notEmpty(CharSequence s) {
		return ! isEmpty(s);
	}

	public final static List<String> splitString(String s) {
		
		if (s == null) return Collections.emptyList();
		
		List<String> result = new ArrayList<String>();
		StringTokenizer loop = new StringTokenizer(s, "{([ \t,])}");
		while (loop.hasMoreTokens()) {
			result.add(loop.nextToken());
		}
		
		return result;
	}
	
	/**
	 * Trims if the argument is not null. 
	 * returns null, if the argument is null or of zero length after trimming.
	 * @param s
	 * @return
	 */
	public static String trimAndNormalizeToNull(String s) {
		if (s!=null && s.length()>0) {
			s=s.trim();
			if (s.length()>0) {
				return s;
			}
		}
		return null;
	}

	/**
	 * Trims tail of argument if it is not null. 
	 * returns null, if the argument is null or of zero length after trimming.
	 * @param s
	 * @return
	 */
	public static String trimTailAndNormalizeToNull(String s) {
		if (s!=null && s.length()>0) {
			int p = s.length();
			while (p>0 && s.charAt(p-1)==' ') p--;
			if (p>0) {
				return s.substring(0,p);
			}
		}
		return null;
	}

	/**
	 * remove blanks and normalize to null
	 * @param lowerCase
	 * @return
	 */
	public static String lowerCaseTrimIdentifierAndNormalizeToNull(String s) {
		if (s!=null) {
			// remove all non alpha
			StringBuilder sb = new StringBuilder(s.length());
			for (int i = 0; i<s.length();i++) {
				char c = Character.toLowerCase(s.charAt(i));
				if ((c>='a' && c<='z') || (c>='0' && c<='9')) {
					sb.append(c);
				}
			}
			s = s.toString();
			if (s.length()==0) {
				s=null;
			}
		}
		return s;
	}
	
	/**
	 * Exception safe getString method for resource bundles. Catches any exception and if so returns
	 * the string key with leading and trailing ??? 
	 */
	public static String getString(ResourceBundle b, String key, Object... args) {
		try {
			String m = b.getString(key);
			if (args.length==0) {
				return m;
			}
			return MessageFormat.format(m, args);
		} catch (Exception e) {
			return "???"+key+"???";
		}
	}


}
