/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.engine;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId.Segment;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;

/**
 * This filter makes sure that {@link Z2JupiterTestable} tests are not considered by other test engines on the
 * client and that they are completely ignore by the {@link Z2JupiterTestEngine} on the back end (and instead 
 * handled by whatever other test engine feels responsible. 
 * 
 * This is the main ingredient making sure we can co-exist in peace.
 * 
 * @author hb
 */
public class Z2JupiterPostDiscoveryFilter implements PostDiscoveryFilter {
	private final static Logger LOG = Logger.getLogger(Z2JupiterPostDiscoveryFilter.class.getName());
	public static ThreadLocal<Set<Class<?>>> discovered = ThreadLocal.withInitial(HashSet::new);

	@Override
	public FilterResult apply(TestDescriptor d) {
		Segment initial = d.getUniqueId().getSegments().get(0);
		if (Z2JupiterTestEngine.isEnabled()) {
			// if on client, filter anything that is NOT z2 Jupiter but covered by z2 Jupiter
			if (!Z2JupiterTestEngine.Z2_JUPITER.equals(initial.getValue()) && isInZ2UnitDiscovery(d)) {
				LOG.fine("Client filtering "+d.getDisplayName()+"/"+d.getUniqueId());
				// exclude
				return FilterResult.excluded("handled on client by z2 Jupiter");
			}
		} else {
			// if on backend, filter anything that is z2 jupiter
			if (Z2JupiterTestEngine.Z2_JUPITER.equals(initial.getValue())) {
				LOG.fine("Backend filtering "+d.getDisplayName()+"/"+d.getUniqueId());
				// exclude
				return FilterResult.excluded("excluded on backend by z2 Jupiter");
			}
		}
		return FilterResult.included("ok");
	}

	private boolean isInZ2UnitDiscovery(TestDescriptor d) {
		TestSource s = d.getSource().orElse(null);
		if (s!=null && s instanceof ClassSource) {
			ClassSource cs = (ClassSource) s;
			if (discovered.get().contains(cs.getJavaClass())) {
				// discovered by z2unit!
				return true;
			}
		}
		return d.getParent().isPresent() && isInZ2UnitDiscovery(d.getParent().get());
	}

}
