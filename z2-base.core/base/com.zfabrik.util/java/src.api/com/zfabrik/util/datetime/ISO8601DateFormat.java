/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.util.datetime;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class ISO8601DateFormat {

	private static Calendar _parse(String date) {
		int a = 0, e, f;
		// Sequence: yyyy-MM-ddTHH:mm:ss,ffff{Z|+HH:mm|-HH:mm}
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.clear();
		try {
			// yyyy
			e = date.indexOf('-', a);
			f = (e < 0 ? date.length() : e);
			cal.set(Calendar.YEAR, Integer.parseInt(date.substring(a, f)));
			if (e < 0)
				return cal;
			a = e + 1;
			// MM
			e = date.indexOf('-', a);
			f = (e < 0 ? date.length() : e);
			cal.set(Calendar.MONTH, Integer.parseInt(date.substring(a, f)) - 1);
			if (e < 0)
				return cal;
			a = e + 1;
			// dd
			e = date.indexOf('T', a);
			f = (e < 0 ? date.length() : e);
			cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(a, f)));
			if (e < 0)
				return cal;
			a = e + 1;
			// HH
			e = date.indexOf(':', a);
			f = (e < 0 ? date.length() : e);
			cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(date.substring(a, f)));
			if (e < 0)
				return cal;
			a = e + 1;
			// mm
			e = date.indexOf(':', a);
			// could be from the time zone
			int h = date.indexOf('Z', a);
			if (h < 0)
				h = date.indexOf('+', a);
			if (h < 0)
				h = date.indexOf('-', a);
			// in case we found no colon or we found it after the time zone, the next token starts at the time zone
			if (e<0 || (h>0 && e>h)) e=h;
			f = (e < 0 ? date.length() : e);
			cal.set(Calendar.MINUTE, Integer.parseInt(date.substring(a, f)));
			if (e < 0)
				return cal;
			a = e + 1;
			if (date.charAt(e) == ':') {
				// ss
				e = date.indexOf('.', a);
				if (e < 0)
					e = date.indexOf('Z', a);
				if (e < 0)
					e = date.indexOf('+', a);
				if (e < 0)
					e = date.indexOf('-', a);
				f = (e < 0 ? date.length() : e);
				cal.set(Calendar.SECOND, Integer.parseInt(date.substring(a, f)));
				if (e < 0)
					return cal;
				a = e + 1;
				if (date.charAt(e) == '.') {
					// ffff
					e = date.indexOf('Z', a);
					if (e < 0)
						e = date.indexOf('+', a);
					if (e < 0)
						e = date.indexOf('-', a);
					f = (e < 0 ? date.length() : e);
					// max precision is ms.
					int g = Math.min(f, a + 3);
					int ms = Integer.parseInt(date.substring(a,g));
					// short string requires decimal left shift 
					while (g++<a+3) ms*=10;
					cal.set(Calendar.MILLISECOND,ms); 
					a = e + 1;
				}
			}
			if (e>=0) {
				// time zone
				char c = date.charAt(e);
				int tzh = 0, tzm = 0;
				if (c != 'Z') {
					// not utc. need to parse time zone offset.
					e = date.indexOf(':', a);
					f = (e < 0 ? date.length() : e);
					tzh = Integer.parseInt(date.substring(a, f));
					if (e >= 0) {
						a = e + 1;
						tzm = Integer.parseInt(date.substring(a));
					}
					if (c == '+') {
						cal.add(Calendar.HOUR, -tzh);
						cal.add(Calendar.MINUTE, -tzm);
					} else {
						cal.add(Calendar.HOUR, tzh);
						cal.add(Calendar.MINUTE, tzm);
					}
				}
			}
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException("Invalid iso8601 date:" + date, nfe);
		}
		return cal;
	}

	public static Date parse(String date) {
		return _parse(date).getTime();
	}
	
	public static int NO_MILLIS = 1;
	
	private static String PT_FULL = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tS.%1$tLZ";
	private static String PT_NOMILLIS = "%1$tY-%1$tm-%1$tdT%1$tH:%1$tM:%1$tSZ";
	
	public static String toISO8601String(Date d,int flags) {
	    // convert to UTC
	    Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
	    cal.setTime(d);
    	// 2010-06-17T20:45:00,789+00:00
	    if (cal.get(Calendar.MILLISECOND)==0 || (flags & NO_MILLIS)!=0) 
	    	return String.format(PT_NOMILLIS,cal);
    	return String.format(PT_FULL,cal);
}

	
	public static String toISO8601String(Date d) {
	    	return toISO8601String(d, 0);
	}

	private static void _check(String d) {
		Calendar c = _parse(d);
		System.out.println(" in: "+d);
		System.out.println("out: "+toISO8601String(c.getTime()));
	}
	
	public static void main(String[] args) {
		_check("2010-06-17T21:20:15.123+02:00");
		_check("2010-06-17T21:20:15+02:00");
		_check("2010-06-17T21:20+02:00");
		_check("2010-06-17T21:20:15.123");
		_check("2010-06-17T21:20:15");
		_check("2010-06-17T21:20:15Z");
		_check("2010-06-17T21:20Z");
		_check("2010-06-17");
		_check("2010-06");
		_check("2010");
	}
}
