/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.tests;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.zfabrik.impl.workers.home.CommandLineParser;

public class CommandLineParserTest {

	@Test
	public void simpleSplit() {
		Assert.assertEquals(
			Arrays.asList("this","is","a","simple","  test ","with","stuff"),
			CommandLineParser.split(" this is   a simple \"  test \" with stuff  ")
		);
		Assert.assertEquals(
			Arrays.asList("this","is","a","simple","  test ","with","stuff"),
			CommandLineParser.split("this is   a simple \"  test \" with stuff  ")
		);
	}

	@Test
	public void escapingSplit() {
		Assert.assertEquals(
			Arrays.asList("dies","ist","ein","simpler"," \" Test ","mit","krams"),
			CommandLineParser.split(" dies ist   ein simpler \" \\\" Test \" mit krams  ")
		);
		Assert.assertEquals(
			Arrays.asList("dies","ist \\ \" ","ein","simpler"," \" Test ","mit","krams"),
			CommandLineParser.split(" dies \"ist \\\\ \\\" \"    ein simpler \" \\\" Test \" mit krams  ")
		);
	}

	@Test
	public void cornerCases() {
		Assert.assertEquals(
			Arrays.asList(),
			CommandLineParser.split("")
		);
		Assert.assertEquals(
			Arrays.asList(""),
			CommandLineParser.split("\"\"")
		);
	}

	@Test(expected=IllegalArgumentException.class)
	public void unterminatedQuote() {
		CommandLineParser.split("\"");
	}

	@Test(expected=IllegalArgumentException.class)
	public void unusedEscape() {
		CommandLineParser.split("\"\\");
	}

}
