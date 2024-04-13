/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.z2unit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sortable;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import com.zfabrik.z2unit.annotations.Z2UnitTest;
import com.zfabrik.z2unit.impl.Protocol;
import com.zfabrik.z2unit.impl.Protocol.SEvent;

/**
 * JUnit4 Unit test runner that, when invoked client-side, sends a request server-side to run unit tests of the given class on the server side.
 * When executed server side this runner delegates all work to the default implementation. A server-side JUnit RunListener logs JUnit events
 * and streams them back to the client side.
 * <p>
 * The client-side execution of this test runner parses streamed events and replays them as JUnit events to the local JUnit execution context, so that
 * to the client-side the results "look" like local execution events.
 * <p>
 * Tests executed with this test runner should be annotated using {@link Z2UnitTest} to configure the Java component that holds the unit test 
 * class like this. 
 * <pre>
 * &#64;RunWith(Z2UnitTestRunner.class)
 * &#64;Z2UnitTest(componentName="z2unit.sometests")
 * public class SomeTests {
 * 
 * 	&#64;BeforeClass
 * 		public static void beforeClass() {
 * 		System.out.println("beforeClass");
 * 	}
 * 
 * 	&#64;AfterClass
 * 	public static void afterClass() {
 * 		System.out.println("afterClass");
 * 	}
 *
 *	&#64;Before
 *	public void before() {
 *		System.out.println("before");
 *	}
 *
 *	&#64;After
 *	public void after() {
 *		System.out.println("after");
 *	}
 *	
 *	&#64;Test
 *	public void succeedingTest() {
 *		Assert.assertTrue("This test succeeds", true);
 *	}
 *}
 *</pre>
 *
 *This unit test runner also supports filtered and sorted test execution, i.e. in particular single method tests. 
 *<p>
 * The Z2 Unit feature is not usable for <a href="https://junit.org/junit5/">JUnit 5</a> tests. While 
 * this feature will be preserved for existing JUnit 4 based tests, new implementations should use 
 * the Z2 Jupiter feature (see <a href="/javadoc/com.zfabrik.dev.z2jupiter!2Fjava/api/com/zfabrik/dev/z2jupiter/Z2JupiterTestable.html">Z2JupiterTestable</a>).
 * 
 *
 * @see Z2UnitTest
 * @author hb
 *
 *
 */

public class Z2UnitTestRunner extends Runner implements Filterable, Sortable {
	private final static Logger LOG = Logger.getLogger(Z2UnitTestRunner.class.getName());

	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
	
	private static final String HTTP_LOCALHOST_8080_Z2UNIT_RUN = "http://localhost:8080/z2unit/run";
	private static final String Z2UNIT_Z2UNIT_URL = "z2unit.z2unitUrl";
	private static final String Z2UNIT_Z2UNIT_USER = "z2unit.z2unitUser";
	private static final String Z2UNIT_Z2UNIT_PASSWORD = "z2unit.z2unitPassword";
	private static final String Z2UNIT_COMPONENT_NAME = "z2unit.componentName";
	private static final String Z2UNIT_CLASS_NAME = "z2unit.className";

	private static final String Z2UNIT_PROPERTIES = "z2unit.properties";
	private static final String ISO8859_1 = "ISO8859-1";
	
	private Class<?> clz;
	
	// config
	private String componentName;
	private String z2unitUrl;
	private String z2unitUser;
	private String z2unitPassword;
	private String className;
	private Class<? extends Runner> runWithClass;
	
	// the runner we are abstracting
	// to remote invocations.
	private Runner runWith;

	public Z2UnitTestRunner(Class<?> klass) throws InitializationError {
		this.clz = klass;
		this.readConfig();
		try {
			if (this.runWithClass!=Runner.class) {
				this.runWith = this.runWithClass.getConstructor(Class.class).newInstance(this.clz);
			} else {
				this.runWith = new BlockJUnit4ClassRunner(this.clz);
			}
        } catch (InitializationError e) {
            // InitializationError must not be wrapped!            
            throw e;
		} catch (Exception e) {
			throw new InitializationError(e);
		}
	}
	
	// basic methods

	@Override
	public void filter(Filter filter) throws NoTestsRemainException {
		if (this.runWith instanceof Filterable) {
			((Filterable)this.runWith).filter(filter);
		}
	}
	
	@Override
	public void sort(Sorter sorter) {
		if (this.runWith instanceof Sortable) {
			((Sortable)this.runWith).sort(sorter);
		}
	}

	@Override
	public Description getDescription() {
		return this.runWith.getDescription();
	}

	@Override
	public int testCount() {
		return this.runWith.testCount();
	}

	//
	// The actual running of tests: This is where we intercept
	// to have invocations forwarded to remote.
	// 
	
	@Override
	public void run(RunNotifier runNotifier) {
		
		// The flow is as follows:
		// a) get the locally sorted, filtered, or otherwise prepared description and send it over.
		// b) On the remote side, the description is used to configure the (custom or not) runner.
		// c) Events are streamed back.
		runTest(runNotifier);
	}
	
	
	//
	// run the remote test by sending the description and listening to streamed by notifications
	//
	private void runTest(RunNotifier runNotifier) {
		try {
			String url = this.z2unitUrl+"?componentName="+URLEncoder.encode(this.componentName,ISO8859_1)+"&className="+URLEncoder.encode(this.className,ISO8859_1);

			LOG.info("Running test at URL "+url);
			URL u = new URL(url);
			HttpURLConnection hc = (HttpURLConnection) u.openConnection();
			if (this.z2unitUser!=null && this.z2unitPassword!=null) {
				hc.setRequestProperty(
					"Authorization",
					"Basic " + Base64.getEncoder().encodeToString((this.z2unitUser + ":" + this.z2unitPassword).getBytes(StandardCharsets.ISO_8859_1))
				);
			}

			
			HttpURLConnection.setFollowRedirects(true);
			
			hc.setDoOutput(true);
			hc.setRequestProperty("Content-Type", APPLICATION_OCTET_STREAM);	
			hc.setRequestMethod("POST");
			
			// send the test description
			try (Writer writer = new OutputStreamWriter(hc.getOutputStream(), "utf-8")) {
				Protocol.serialize(getDescription(), writer);
			}
			
			hc.connect();
			try {
				int status = hc.getResponseCode();
				// read response
				String enc = hc.getContentEncoding();
				if (enc==null) {
					enc = ISO8859_1;
				}
				String contentType=hc.getContentType();
				if (contentType!=null) {
					int p=contentType.indexOf(';');
					if (p>=0) {
						contentType = contentType.substring(0,p);
					}
				}
				
				if (status==200 && APPLICATION_OCTET_STREAM.equals(contentType)) {
					// stream in event response
					try (Reader in = new InputStreamReader(hc.getInputStream(),enc)) {
						SEvent event;
						while ((event=Protocol.deSerializeEvent(in))!=null) {
							LOG.fine("Received test event "+event.getType());
							replay(event,runNotifier);
						}
					}
				} else  {
					LOG.info("Receiving fail response "+status+" ("+contentType+")");
					// error case. Simply capture response and wrap in exception
					StringWriter w = new StringWriter();
					Reader in = new InputStreamReader(hc.getInputStream(),enc);
					try {
						char[] buffer = new char[16384];
						int l;
						while ((l=in.read(buffer))>=0) {
							w.write(buffer,0,l);
						}
					} finally {
						in.close();
					}
					String content = normalize(w.toString());
				
					throw new RuntimeException(
						 "Test request failed with status "+status
						+" ("+hc.getResponseMessage()+")"
						+(content!=null? "\n"+content:"")
					);
				}
			} finally {
				hc.disconnect();
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to run remote test",e);
		}
	}

	// replay remote results
	private void replay(SEvent e, RunNotifier runNotifier) throws IOException {
		String type = e.getType();
		if (Protocol.EVENT_TEST_ASSUMPTION_FAILURE.equals(type)) {
			runNotifier.fireTestAssumptionFailed(e.getFailure().toFailure());
		} else
		if (Protocol.EVENT_TEST_FAILURE.equals(type)) {
			runNotifier.fireTestFailure(e.getFailure().toFailure());
		} else
		if (Protocol.EVENT_TEST_FINISHED.equals(type)) {
			runNotifier.fireTestFinished(e.getDescription());
		} else
		if (Protocol.EVENT_TEST_IGNORED.equals(type)) {
			runNotifier.fireTestIgnored(e.getDescription());
		} else
		if (Protocol.EVENT_TEST_RUN_FINISHED.equals(type)) {
			runNotifier.fireTestRunFinished(e.getResult().toResult());
		} else
		if (Protocol.EVENT_TEST_RUN_STARTED.equals(type)) {
			runNotifier.fireTestRunStarted(e.getDescription());
		} else
		if (Protocol.EVENT_TEST_STARTED.equals(type)) {
			runNotifier.fireTestStarted(e.getDescription());
		}
	}

	// go and fish for config
	private void readConfig() {
		// check for config annotation
		Z2UnitTest z2ut = this.clz.getAnnotation(Z2UnitTest.class);
		String cfgFile=null;
		if (z2ut!=null) {
			this.className = normalize(z2ut.className());
			this.componentName = normalize(z2ut.componentName());
			this.z2unitUrl = normalize(z2ut.z2unitUrl());
			this.z2unitUser = normalize(z2ut.z2unitUser());
			this.z2unitPassword = normalize(z2ut.z2unitPassword());
			this.runWithClass = z2ut.runWith();
			cfgFile = normalize(z2ut.configFile());
		}

		// read props
		String cfg; 
		if (cfgFile==null) {
			cfg = Z2UNIT_PROPERTIES;
		} else {
			cfg = cfgFile;
		}
		if (cfg!=null) {
			Properties p = new Properties();
			try {
				InputStream fin = getClass().getClassLoader().getResourceAsStream(cfg);
				if (fin==null) {
					if (cfgFile!=null) {
						throw new IllegalArgumentException("Configuration "+cfgFile+" not found on classpath");
					}
				} else {
					try {
						p.load(fin);
						System.getProperties().putAll(p);
					} finally {
						fin.close();
					}
				}
			} catch (IOException ioe) {
				throw new RuntimeException("Failed to read config file "+cfg,ioe);
			}
		}		
		//
		Optional.ofNullable(normalize(System.getProperty(Z2UNIT_CLASS_NAME))).ifPresent(s->this.className=s);
		Optional.ofNullable(normalize(System.getProperty(Z2UNIT_COMPONENT_NAME))).ifPresent(s->this.componentName=s);
		Optional.ofNullable(normalize(System.getProperty(Z2UNIT_Z2UNIT_PASSWORD))).ifPresent(s->this.z2unitPassword=s);
		Optional.ofNullable(normalize(System.getProperty(Z2UNIT_Z2UNIT_USER))).ifPresent(s->this.z2unitUser=s);
		Optional.ofNullable(normalize(System.getProperty(Z2UNIT_Z2UNIT_URL))).ifPresent(s->this.z2unitUrl=s);
		
		if (this.className==null) {
			// compute
			this.className = this.clz.getName();
		}
		if (this.componentName==null) {
			throw new IllegalArgumentException("Specify component name containing the server side test class either with the @Z2UnitTest annotation or in the z2unit config file");
		}
		if (this.z2unitUrl==null) {
			this.z2unitUrl = HTTP_LOCALHOST_8080_Z2UNIT_RUN;
		}
	}

	
	//
	// helper
	//
	private String normalize(String in) {
		if (in!=null) {
			in = in.trim();
			if (in.length()==0) {
				in = null;
			}
		}
		return in;
	}

}
