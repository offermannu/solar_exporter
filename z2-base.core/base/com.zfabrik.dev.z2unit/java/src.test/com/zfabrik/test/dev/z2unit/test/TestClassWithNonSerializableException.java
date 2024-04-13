/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.dev.z2unit.test;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.zfabrik.z2unit.Z2UnitTestRunner;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

/**
 * Run with nested parameterized
 */
@RunWith(Z2UnitTestRunner.class)
@Z2UnitTest(componentName="com.zfabrik.dev.z2unit")
@Ignore
public class TestClassWithNonSerializableException {

	// a non-serializable exception
	@SuppressWarnings("serial")
	private static class NonSerializableException extends RuntimeException {
		
		private static class Custom {
			public Custom() {}
		}
		
		private Custom custom = new Custom();
	}

	// a non-deserializable exception
	@SuppressWarnings("serial")
	private static class NonDeSerializableException extends RuntimeException {
		
		private static class Custom implements Externalizable {
			
			public Custom() {}
			
			@Override
			public void writeExternal(ObjectOutput out) throws IOException {
				// nothing
			}
			@Override
			public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
				throw new RuntimeException("FAIL, FAIL, FAIL!");
			}
		}
		
		private Custom custom = new Custom();
	}
	
	
	@Test
	public void nonSerializable() {
		throw new NonSerializableException();
	}
	
	@Test
	public void nonDeSerializable() {
		throw new NonDeSerializableException();
	}
	
}
