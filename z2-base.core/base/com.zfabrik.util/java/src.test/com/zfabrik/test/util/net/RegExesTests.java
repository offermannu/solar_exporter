/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.util.net;

import java.util.regex.Pattern;

import org.junit.Test;

import junit.framework.Assert;

import com.zfabrik.util.net.ParsedURL;


public class RegExesTests {

	@Test
	public void testWithHost() {
		String res = Pattern.compile(new ParsedURL("host.de").toRegEx()).matcher("http://host.de:8000/xyz").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH/xyz".equals(res));
		res = Pattern.compile(new ParsedURL("host.de").toRegEx()).matcher("/xyz").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "/xyz".equals(res));
	}


	@Test
	public void testWithoutHost() {
		String res = Pattern.compile(new ParsedURL("/zu").toRegEx()).matcher("/zu").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH".equals(res));
		res = Pattern.compile(new ParsedURL("/zu").toRegEx()).matcher("/zu/hause").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH/hause".equals(res));
		res = Pattern.compile(new ParsedURL("/zu").toRegEx()).matcher("http://xyz/zu/hause").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "http://xyz/zu/hause".equals(res));
	}

	@Test
	public void testWithHostAndPath() {
		String res = Pattern.compile(new ParsedURL("host.de/xyz").toRegEx()).matcher("http://host.de:8000/xyz").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH".equals(res));
		res = Pattern.compile(new ParsedURL("host.de/xyz").toRegEx()).matcher("http://host.de:8000/xyz/abcd").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH/abcd".equals(res));
		res = Pattern.compile(new ParsedURL("host.de/xyz").toRegEx()).matcher("http://host.de/xyz/abcd").replaceAll("MATCH");
		Assert.assertTrue("Wrong result "+res, "MATCH/abcd".equals(res));
	}
}
