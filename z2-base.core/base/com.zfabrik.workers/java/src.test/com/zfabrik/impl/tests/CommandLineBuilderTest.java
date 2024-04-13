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

import java.util.HashMap;
import java.util.Properties;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.zfabrik.impl.workers.home.CommandLineBuilder;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * Tests for the command line builder
 * @author hb
 *
 */
public class CommandLineBuilderTest {

	private static final String WORKER_LAUNCHER = CommandLineBuilder.WORKER_LAUNCHER;
	private Properties props = new Properties();
	private Properties sysp;

	@SuppressWarnings("serial")
	@Before
	public void setup() {
		// preserve sys props
		this.sysp = new Properties();
		this.sysp.putAll(System.getProperties());
		props.putAll(new HashMap<String,String>() {{
			put("worker.debug.port","5100");
			put("worker.jmx.port","7800");
			put("worker.process.vmOptions","-Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");

		}});
		System.setProperty("java.class.path","CLP");
		System.setProperty("java.library.path","LIB");
	}

	@After
	public void cleanup() {
		System.setProperties(sysp);
	}


	@Test
	public void ordinaryCommandLine() {
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{"java","-cp","CLP","-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void commandLineWithBlanks() {
		props.setProperty("worker.process.vmOptions","\"-Dwilli=ich mach mich nass\" -Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{"java","-cp","CLP","-Djava.library.path=LIB","-Dwilli=ich mach mich nass","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void classPathWithBlanks() {
		System.setProperty("java.class.path","a/b d:/du mich auch/c");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{"java","-cp","a/b d:/du mich auch/c","-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void classPathSubstitutedWithBlanks() {
		System.setProperty("java.class.path","a/b d:/du mich auch/c");
		props.setProperty("worker.class.path", "extra:{0}");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{"java","-cp","extra:a/b d:/du mich auch/c","-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void libraryPathWithBlanks() {
		System.setProperty("java.library.path","a/b d:/du mich auch/c");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{"java","-cp","CLP","-Djava.library.path=a/b d:/du mich auch/c","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void debugCommandLine() {
		System.setProperty(IWorkerProcess.WORKER_DEBUG,"true");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{
					"java","-cp","CLP",
					 "-Xdebug","-Xnoagent","-Xrunjdwp:transport=dt_socket,suspend=n,server=y,address=5103",
					"-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void debugCustomCommandLine() {
		System.setProperty(IWorkerProcess.WORKER_DEBUG,"true");
		props.put(IWorkerProcess.DEBUG_PARAMS, "-Dspecial={0}");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{
					"java","-cp","CLP",
					 "-Dspecial=5103",
					"-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void jmxCommandLine() {
		System.setProperty(IWorkerProcess.WORKER_REMOTE_JMX,"true");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{
					"java","-cp","CLP",
					"-Dcom.sun.management.jmxremote.port=7803",
					"-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}

	@Test
	public void jmxCustomCommandLine() {
		System.setProperty(IWorkerProcess.WORKER_REMOTE_JMX,"true");
		props.put(IWorkerProcess.JMX_PARAMS, "-Ddussel={0}");
		CommandLineBuilder b = CommandLineBuilder.fromConfig("env/worker", 3, props);
		String[] cli = b.toStringArray();
		Assert.assertArrayEquals(
			new String[]{
					"java","-cp","CLP",
					"-Ddussel=7803",
					"-Djava.library.path=LIB","-Xmx128m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError", "-Dcom.zfabrik.process.worker=env/worker", WORKER_LAUNCHER},
			cli
		);
	}


}
