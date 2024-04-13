/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.zfabrik.components.IDependencyComponent;


/**
 * Annotation marking a test class as executable via the Z2 Jupiter Test Engine. The  
 * Z2 Jupiter Test Engine performs test execution in the actual server-side application runtime environment
 * rather than in the runtime environment of the client requesting the test.
 * <p>
 * To do so, requests for test discovery and execution are sent from the client,
 * where the test is initiated (e.g. your IDE), to 
 * the Web application <code>com.zfabrik.dev.z2jupiter/web</code> listening
 * on the context path <code>/z2jupiter</code> and running as part of <code>environment/webWorkerUp</code>
 * by default.
 * <p>
 * Technically, the Z2 Jupiter Test Engine masks test execution of annotated classes
 * for non-execution locally and for normal execution remotely and reports test events
 * as if executed locally, so that, from a local client perspective the execution
 * is as if it happened locally, while the execution on the remote end works ignoring
 * Z2 Jupiter and so in principle, supports any other test engine or configuration model.
 * <p>
 * You may combine this annotation with other test engine annotations.
 * <p>
 * Using this annotation as well as through system properties and environment settings
 * various aspects of the execution may be configured.
 * <p>
 * One particular aspect of execution is the remote location to test one that 
 * may be set using the {@link #url()} attribute. This setting may be overwritten,
 * in order of preference by
 * <ul>
 * <li>The system property <code>com.zfabrik.dev.z2jupiter.url</code></li>
 * <li>The environment variable <code>Z2_JUPITER_URL</code></li>
 * </ul>
 * <p>
 * Remote access to test discovery and execution is protected by authentication.
 * User name and password may be specified by setting the {@link #user()} and
 * {@link #password()} attribute respectively. Authentication is verified against the
 * default "realm" that is configured in <code>environment/webUsers</code> (and
 * may be changed by adapting the built-in Jetty Web server configuration).
 * Alternatively, username and password may be specified by system properties
 * and environment properties, where system properties take preference.
 * <p/>
 * For username:  
 * <ul>
 * <li>The system property <code>com.zfabrik.dev.z2jupiter.user</code></li>
 * <li>The environment variable <code>Z2_JUPITER_USER</code></li>
 * </ul>
 * <br/>
 * For password:  
 * <ul>
 * <li>The system property <code>com.zfabrik.dev.z2jupiter.password</code></li>
 * <li>The environment variable <code>Z2_JUPITER_PASSWORD</code></li>
 * </ul>
 * <p>
 * Note: In order to make remote execution from non-localhost clients possible, you may
 * need to change z2 configuration to allow such remote access or use a means tunneling, 
 * like SSH tunneling (see also <a href="https://redmine.z2-environment.net/projects/z2-environment/wiki/How_to_Secure_a_Z2_Installation">How to Secure a Z2 Installation</a>)
 * <p>
 * Configuration for a unit test class that is to run via the Z2 Jupiter Test Engine simply
 * requires to specify this annotation:
 * </p>
 * <pre><code>
 * &#64;Z2UnitTestable(componentName="com.acme.modulex/java")
 * public class SomeTests {
 * 	&#64;Test
 * 	public void testThis() throws Exception {
 * 		// some test code
 * 	}
 * }
 * </code></pre>
 * <p>
 * Sometimes it can be handy to initialize other components before running a test (e.g. a Spring application 
 * context). Instead of using a super class to do static initialization, you can instead use the {@link #dependencies()}
 * attribute to specify Z2 components to be prepared before the test run. For example a test that requires a Spring application
 * context to be initialized and makes use of Spring configuration could look like this: 
 *</p>
 * <pre><code>
 * &#64;Z2UnitTestable(
 *   componentName="com.acme.modulex/java",
 *   dependencies={"com.acme.modulex/applicationContext"}
 * )
 * &#64;Configurable
 * public class BusinessTests {
 * 
 *   &#64;Autowired
 *   private SomeService someService;
 *   
 *   &#64;Test &#64;Transactional
 *   public void testSomething() {
 *     // ...
 *   }
 * 
 * }
 * </code></pre>
 *
 * The Z2 Jupiter Engine feature builds on the <a href="https://junit.org/junit5/">JUnit 5</a> API and succeeds 
 * the Z2 Unit feature (see <a href="/javadoc/com.zfabrik.dev.z2unit!2Fjava/api/com/zfabrik/z2unit/Z2UnitTestRunner.html">Z2UnitTestRunner</a>).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface Z2JupiterTestable {
	
	/**
	 * Name of the java component that contains the test class. A short form reduced to the module name may be used for java components
	 * ending with "/java".
	 * 
	 * @return
	 */
	String componentName() default "";

	/**
	 * URL to send the test request to. Defaults to <code>http://localhost:8080/z2unit.jupiter/run</code>. 
	 * The URL may alternatively be defined by the system property <code>com.zfabrik.dev.z2jupiter.url</code> or the 
	 * environment variable <code>Z2_JUPITER_URL</code>.
	 * 
	 * @return
	 */
	String url() default "http://localhost:8080/z2jupiter";

	/**
	 * Username to send for authentication. Defaults to "z*".
	 * 
	 * The username may alternatively be defined by the system property <code>com.zfabrik.dev.z2jupiter.user</code> or the 
	 * environment variable <code>Z2_JUPITER_USER</code>.
	 * 
	 * @return
	 */
	String user() default "z*";

	/**
	 * Password name to send for authentication. Defaults to "z". 
	 * 
	 * The password may alternatively be defined by the system property <code>com.zfabrik.dev.z2jupiter.password</code> or the 
	 * environment variable <code>Z2_JUPITER_PASSWORD</code>.
	 */
	String password() default "z";

	/**
	 * Runtime dependencies specified as component names. Before executing a test on the z2 side, these components
	 * will be looked up as {@link IDependencyComponent} and, if successful, be asked to {@link IDependencyComponent#prepare()}.
	 */
	String[] dependencies() default {};

}
