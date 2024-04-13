/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.util;

public class StringUtil {

	/**
	 * let s1 = xy and s2 = yz (where x and y and z are strings of length >= 1)
	 * find the overlapping part y and return the rest which is z, 
	 * so that s1 + minusOverlap(s1, s2) = xyz 
	 * 
	 * @param s1 string = xy
	 * @param s2 string = yz
	 * @return z the remaining fraction of s2 without the prefix that overlaps with s1 suffix 
	 */
	public static String minusOverlap(String s1, String s2) {
		if (s1 == null || s2 == null) return null;
		if (s1.length() == 0) return s2;
		if (s2.length() == 0) return "";
		
		String result = s2;
		
		// the leading character of s2 (i.e. s2.charAt(0)) must occur somewhere in s1 (maybe several times)
		char leading = s2.charAt(0);
		
		// all occurrences of 'leading' in s1 are candidates for the overlapping part
		int matchPos = 0;
		
		boolean found = false;
		while (! found && matchPos != -1) {
			
			// find next matching positions
			matchPos = s1.indexOf(leading, matchPos);
			if (matchPos >= 0) {
				
				String overlap = s1.substring(matchPos);
				if (s2.startsWith(overlap)) {
					found = true;
					int len = overlap.length();
					// result is the s2 w/o the overlap
					// consider the case that s1 = xy and s2 = y, in this case the remaing fraction is just the empty string 
					result = s2.length() > len? s2.substring(len) : "";
				} else {
					// move on
					matchPos++;
				}
			}
		}
		
		return result;
	}
	
	public static <T> T getFirstNotNull(T... ts) {
		for (T t : ts) {
			if (t != null) return t;
		}
		return null;
	}
	
	private static void check(String s1, String s2, String expected) {
		System.out.printf("check %s minusOverlap %s", q(s1), q(s2));
		String result = minusOverlap(s1, s2);
		if (expected == null && result == null || expected.equals(result)) {
			System.out.printf(" = %s OK\n", q(result));
		} else {
			System.out.printf(" = %s FAILED. Expected = %s\n", q(result), q(expected));
		}
	}
	
	private static String q(String s) {
		return s == null? "null" : "'" + s + "'";
	}
	
	public static void main(String[] args) {
		check(null, null, null);
		check(null, "", null);
		check("a", null, null);
		check("", "", "");
		check("abc", "", "");
		check("abcabcd", "bcd", "");
		check("abcabcd", "bcdefg", "efg");
		check("aaaaaab", "abc", "c");
		check("abc", "xyz", "xyz");

		System.out.println(":-)");
	}
}
