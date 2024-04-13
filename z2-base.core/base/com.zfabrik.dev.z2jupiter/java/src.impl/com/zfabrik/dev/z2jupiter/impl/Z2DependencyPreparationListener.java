/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;

/**
 * Execution listener enforcing dependencies configured on {@link Z2JupiterTestable#dependencies()}
 */
public class Z2DependencyPreparationListener implements TestExecutionListener {
	private final static Logger LOG = Logger.getLogger(Z2DependencyPreparationListener.class.getName());
	// just to keep them alive during time of the test execution
	private Set<IDependencyComponent> dependencies = new HashSet<IDependencyComponent>();

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		// check whether we are at the class level
		ClassSource clzS = testIdentifier.getSource().filter(s->(s instanceof ClassSource)).map(ClassSource.class::cast).orElse(null);
		if (clzS!=null && clzS.getJavaClass()!=null) {
			// check for the annotation, read the dependencies, and
			// call to prepare them
			Z2JupiterTestable z2u = clzS.getJavaClass().getAnnotation(Z2JupiterTestable.class);
			if (z2u.dependencies()!=null) {
				for (String cn : z2u.dependencies()) {
					IDependencyComponent dc = IComponentsLookup.INSTANCE.lookup(cn, IDependencyComponent.class);
					if (dc!=null) {
						LOG.fine("Preparing dependency "+cn+" on test class "+clzS.getClassName());
						// house keeping
						dependencies.add(dc);
						dc.prepare();
					}
				}
			}
		}
	}
	
}
