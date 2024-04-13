/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.z2unit.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.runner.Runner;

import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.z2unit.Z2UnitTestRunner;

/**
 * <p>
 * Configuration for a unit test class that is run with the z2unit test runner. Use like this:
 * </p>
 * <pre>
 * &#64;RunWith(Z2UnitTestRunner.class)
 * &#64;Z2UnitTest(componentName="myproject/java")
 * public class SomeTests {
 * 	&#64;Test
 * 	public void testThis() throws Exception {
 * 		// some test code
 * 	}
 * }
 * </pre>
 * <p>
 * You may specify custom JUnit unit test runners. For example, a groovy written test using the Spock test specification
 * framework would look like this:
 * </p>
 * <pre>
 * &#64;RunWith(Z2UnitTestRunner.class)
 * &#64;Z2UnitTest(componentName="spock_tests", runWith=Sputnik.class)
 * class HelloSpockZ2 extends Specification {
 * 
 * 	def "my first test"() {
 * 		setup:
 * 			def x = new ArrayList<String>();
 * 		when:
 * 			x.add("Hello")
 * 		then:
 * 			x.size() == 1
 * 	}
 * }
 * </pre>
 * <p>
 * Here the module name is <b>spock_tests</b> and specifying this as component name is a short hand for
 * <b>spock_tests/java</b>. Furthermore note that Sputnik is the Spock-supplied JUnit test runner. Please check the
 * wiki for more information on how to run Groovy on Z2.
 *</p>
 *<p>
 * Sometimes it can be handy to initialize other components before running a test (e.g. a Spring application 
 * context). Instead of using a super class to do static initialization, you can instead use the {@link #dependencies()}
 * attribute to specify Z2 components to be prepared before the test run. For example a test that requires a Spring application
 * context to be initialized and makes use of Spring configuration could look like this: 
 *</p>
 * <pre>
 * &#64;RunWith(Z2UnitTestRunner.class)
 * &#64;Z2UnitTest(
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
 * </pre>
 *
 * @see Z2UnitTestRunner 
 * @author hb
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Z2UnitTest {
	
	/**
	 * A Unit Test Runner to be used when executing the unit test in the target environment. That is, assuming that
	 * the unit test would be annotated as
	 * <pre><code>@RunWith(MyRunner.class)</code></pre>
	 * when run outside of z2 Unit, then, in order to run the 
	 * unit test with the z2 Environment, but using the runner <code>MyRunner</code>, the specification of
	 * the runner is added to this annotation as 
	 * <pre><code>@Z2UnitTest(runWidth=MyRunner.class)</code></pre>
	 * 
	 * In other words: By adding the runner to a declaration of this annotation, you can force a non-default
	 * test runner. This is particularly useful when running tests using JUnit extensions like the Groovy-based
	 * Spock toolkit. In that case for example, you would use the Sputnik JUnit Runner of the Spock
	 * framework.
	 */
	Class<? extends Runner> runWith() default Runner.class;
	
	/**
	 * Name of the java component that contains the test class. A short form may be used for java components
	 * ending with "/java".
	 * Alternatively the component name may be specified using the system property <code>z2unit.componentName</code>
	 * 
	 * @return
	 */
	String componentName() default "";
	
	/**
	 * Name of the test class. Defaults to the current class name. It is however possible to specify a different
	 * test class name to be used server-side. As a result, z2Unit will report test execution events on that class which may not match the
	 * structure of the client side class.
	 * Alternatively the class name may be specified using the system property <code>z2unit.className</code>
	 * 
	 * @return
	 */
	String className()  default "";
	
	/**
	 * URL to send the test request to. Defaults to <code>http://localhost:8080/z2unit/run</code>. 
	 * The URL may alternatively be defined by the system property <code>z2unit.z2unitUrl</code>.
	 * 
	 * @return
	 */
	String z2unitUrl() default "";
	
	/**
	 * Username to send for authentication. Defaults to "z*".
	 * 
	 * The username may alternatively be defined by the system property <code>z2unit.z2unitUser</code>.
	 * 
	 * @return
	 */
	String z2unitUser() default "z*";

	/**
	 * Password name to send for authentication. Defaults to "z". 
	 * 
	 * The password may alternatively be defined by the system property <code>z2unit.z2unitPassword</code>.
	 */
	String z2unitPassword() default "z";

	/**
	 * A z2unit configuration file. Defaults to <code>z2unit.properties</code>. When found on the classpath, the properties specified 
	 * in that file will be loaded into the client-side system properties.
	 * 
	 * @return
	 */
	String configFile() default "";
	
	
	/**
	 * Runtime dependencies specified as component names. Before executing a test on the z2 side, these components
	 * will be looked up as {@link IDependencyComponent} and, if successful, be asked to {@link IDependencyComponent#prepare()}.
	 */
	String[] dependencies() default {};
}
