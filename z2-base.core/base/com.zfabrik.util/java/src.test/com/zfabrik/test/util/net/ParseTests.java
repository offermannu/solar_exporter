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

import junit.framework.Assert;

import org.junit.Test;

import com.zfabrik.util.net.ParsedURL;


public class ParseTests {
	
	@Test
	public void testParseAdress() {
		ParsedURL p; 
		p = new ParsedURL("http://www.zyst.de:8000/path/pather?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));

		p = new ParsedURL(":8000/path/pather?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), null==p.getScheme());
		Assert.assertTrue("Incorrect host: "+p.getHost(), null==p.getHost());
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));
		
		p = new ParsedURL("http://www.zyst.de/path/pather?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), null==p.getPort());
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));

		p = new ParsedURL("www.zyst.de/path/pather?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), null==p.getScheme());
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), null==p.getPort());
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));
	
		p = new ParsedURL("/path/pather?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), null==p.getScheme());
		Assert.assertTrue("Incorrect host: "+p.getHost(), null==p.getHost());
		Assert.assertTrue("Incorrect port: "+p.getPort(), null==p.getPort());
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));
	}

	@Test
	public void testParseTail() {
		ParsedURL p; 
		p = new ParsedURL("http://www.zyst.de:8000?query=2#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), null==p.getPath());
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));

		p = new ParsedURL("http://www.zyst.de:8000/path/pather#frag");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), null==p.getQuery());
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), "frag".equals(p.getFragment()));
		
		
		p = new ParsedURL("http://www.zyst.de:8000/path/pather?query=2");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), "/path/pather".equals(p.getPath()));
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), null==p.getFragment());

		p = new ParsedURL("http://www.zyst.de:8000?query=2");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), null==p.getPath());
		Assert.assertTrue("Incorrect query: "+p.getQuery(), "query=2".equals(p.getQuery()));
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), null==p.getFragment());
		
		p = new ParsedURL("http://www.zyst.de:8000");
		Assert.assertTrue("Incorrect scheme: "+p.getScheme(), "http".equals(p.getScheme()));
		Assert.assertTrue("Incorrect host: "+p.getHost(), "www.zyst.de".equals(p.getHost()));
		Assert.assertTrue("Incorrect port: "+p.getPort(), "8000".equals(p.getPort()));
		Assert.assertTrue("Incorrect path: "+p.getPath(), null==p.getPath());
		Assert.assertTrue("Incorrect query: "+p.getQuery(), null==p.getQuery());
		Assert.assertTrue("Incorrect fragment: "+p.getFragment(), null==p.getFragment());
		
	}
}
