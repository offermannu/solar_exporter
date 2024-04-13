/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple utility to split a blank separated list of
 * command line arguments while supporting quotes and escaping \.
 * That as, any blank separated list of non-blank terms will be split. Multiple blanks will be treated as one. All
 * terms will be trimmed. In order to support terms containing blanks, double quotes may be used. In order to
 * use quotes in those terms, the escaping \ may be used.
 * Examples:
 * <ul>
 * <li><pre> some text that   has a few words </pre> splits to "some","text","that","has","a","few","words"</li>
 * <li><pre> this " has some \" quotes \"" and "something" else</pre> splits to "this"," has some \" quotes \"","and","something","else"</li>
 * </ul>
 *
 */
public class CommandLineParser {
	private enum State { BEFORE, IN_STRING, IN_PARAM, ESCAPED }

	public static List<String> split(String line) {
		State state = State.BEFORE;
		State pree = null;
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (char c : line.toCharArray()) {
			switch (state) {
			case BEFORE:
				if (c!=' ') {
					// we ignore all blanks while looking for the
					// next param
					if (c=='"') {
						// entering string literal
						state = State.IN_STRING;
					} else {
						// entering unquoted param
						state = State.IN_PARAM;
						current.append(c);
					}
				}
				break;
			case IN_PARAM:
				if (c==' ') {
					// push
					result.add(current.toString());
					current.setLength(0);
					// back to before
					state = State.BEFORE;
				} else {
					current.append(c);
				}
				break;
			case IN_STRING:
				if (c=='"') {
					// push
					result.add(current.toString());
					current.setLength(0);
					// back to before
					state = State.BEFORE;
				} else
				if (c=='\\') {
					// switch to escape mode. Any following character will be added as is
					pree = state;
					state = State.ESCAPED;
				} else {
					current.append(c);
				}
				break;
			case ESCAPED:
				// add verbatim
				current.append(c);
				// switch to pre-escape
				state = pree;
				break;
			}
		}
		// finalize
		switch (state) {
		case BEFORE:
			// ok
			break;
		case IN_PARAM:
			// push last one
			result.add(current.toString());
			break;
		case ESCAPED:
			throw new IllegalArgumentException("Expected character following escape character \\ after \""+current.toString()+"\"");
		case IN_STRING:
			throw new IllegalArgumentException("Missing termination of string sequence \""+current.toString()+"\"");
		}
		return result;
	}


}
