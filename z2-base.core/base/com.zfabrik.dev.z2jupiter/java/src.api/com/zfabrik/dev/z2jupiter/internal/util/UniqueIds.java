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

import java.util.List;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.UniqueId.Segment;

/**
 * Working around hidden stuff with UniqueIds
 */
public class UniqueIds {

	/**
	 * Extract a remainder unique id from a unique id.
	 */
	public static UniqueId subUniqueId(UniqueId path, int begin) {
		List<Segment> segments = path.getSegments();
		segments = segments.subList(begin, segments.size());
		UniqueId result = null;
		for (Segment s : segments) {
			if (result==null) {
				result = UniqueId.root(s.getType(), s.getValue());
			} else {
				result = result.append(s);
			}
		}
		return result;
	}

	/**
	 * Concatenate two {@link UniqueId}s
	 */
	public static UniqueId concat(UniqueId base, UniqueId ext) {
		if (base==null) {
			return ext;
		}
		if (ext==null) {
			return base;
		}
		for (Segment s : ext.getSegments()) {
			base = base.append(s);
		}
		return base;
	}

}
