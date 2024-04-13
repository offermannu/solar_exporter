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

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * Just the descriptor root of {@link Z2JupiterTestEngine}.
 */
public class Z2JupiterTestEngineDescriptor extends EngineDescriptor {

	public Z2JupiterTestEngineDescriptor(UniqueId uniqueId) {
		super(uniqueId, Z2JupiterTestEngine.Z2_JUPITER);
	}

}
