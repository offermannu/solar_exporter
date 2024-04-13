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

import static org.junit.platform.commons.util.ReflectionUtils.tryToLoadClass;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.junit.jupiter.engine.discovery.predicates.IsTestFactoryMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestMethod;
import org.junit.jupiter.engine.discovery.predicates.IsTestTemplateMethod;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver.InitializationContext;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClient;
import com.zfabrik.dev.z2jupiter.internal.client.Z2JupiterClientTestDescriptor;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterLauncherDiscoveryRequestDto;
import com.zfabrik.dev.z2jupiter.internal.transfer.Z2JupiterTestPlanDto;
import com.zfabrik.dev.z2jupiter.internal.util.UniqueIds;

/**
 * A {@link TestEngine} that remote tests test classes that are annotated with {@link Z2JupiterTestable}.
 * 
 * When resolving such an annotated test class, this engine resolves to a {@link Z2JupiterRemoteTestDescriptor}. We
 * run a remote discovery for the same class and bind the complete hierarchy beneath the local test descriptor
 * with correspondingly prefixed unique ids of {@link Z2JupiterClientTestDescriptor}. 
 * This is based on the remote serialization implemented with {@link Z2JupiterTestPlanDto} and 
 * related.
 * 
 * At execution, we invoke remote execution and translate events back to the prefixed and nested hierarchy.
 * 
 * NOTE: An alternative execution mode could ignore all test descriptors above the test class level and 
 * reproduce the hierarchy only starting with the test class. 
 */
public class Z2JupiterTestEngine implements TestEngine {
	/**
	 * Test engine id 
	 */
	public static final String Z2_JUPITER = "z2 Jupiter";
	/**
	 * Display name prefix for remoted tests. 
	 */
	public static final String Z2_JUPITER_DISPLAY_PREFIX = "z2:";
	
	// just for simulation and to control whether we 
	// are on the client or the backend to be adapted when ported to Z2
	private static ThreadLocal<Boolean> enabled = ThreadLocal.withInitial(()->Boolean.TRUE);
	public static final String SEGMENT_TYPE_TEST = "test";

	public final static Logger LOG = Logger.getLogger(Z2JupiterTestEngine.class.getName());
	public static final IsTestMethod isTestMethod = new IsTestMethod();
	public static final IsTestFactoryMethod isTestFactoryMethod = new IsTestFactoryMethod();
	public static final IsTestTemplateMethod isTestTemplateMethod = new IsTestTemplateMethod();
	public static final Predicate<Method> isTestOrTestFactoryOrTestTemplateMethod = isTestMethod.or(isTestFactoryMethod).or(isTestTemplateMethod);

	private static final Predicate<Class<?>> isZ2UnitTestable = c-> {
		if (!ReflectionUtils.isPublic(c)) {
			return false;
		}
		if (ReflectionUtils.isAbstract(c)) {
			return false;
		}
		Z2JupiterTestable t = c.getAnnotation(Z2JupiterTestable.class);
		if (t!=null) {
			return true;
		} else {
			return false;
		}
	};
	

	public static void enable() {
		LOG.fine("Z2UnitTestEngine enabled");
		enabled.set(true);
	}

	public static void disable() {
		LOG.fine("Z2UnitTestEngine disabled");
		enabled.set(false);
	}
	
	public static boolean isEnabled() {
		return enabled.get();
	}
	
	public static <T> T withoutTestEngine(Supplier<T> get) {
		Z2JupiterTestEngine.disable();
		try {
			return get.get();
		} finally {
			Z2JupiterTestEngine.enable();
		}
	}

	public static void withoutTestEngine(Runnable run) {
		Z2JupiterTestEngine.disable();
		try {
			run.run();
		} finally {
			Z2JupiterTestEngine.enable();
		}
	}

	/**
	 * Resolver detecting {@link Z2JupiterTestable} test classes.
	 */
	private static class Z2UnitSelectorResolver extends LoggingSelectorResolver {
		
		public Z2UnitSelectorResolver(InitializationContext<TestDescriptor> initContext) {}
		
		/**
		 * Discovery for a class. We do not need to discover deeper as that 
		 * is completely done on the remote side and we only 
		 * nest the result (with all but Z2Jupiter discovery as provided from remote).
		 *  
		 * NOTE: We could not really do this here anyway, as we do not see what
		 * other test engines do here!
		 */
		@Override
		public Resolution resolve(ClassSelector selector, Context context) {
			LOG.fine("Resolving class selector "+selector.getJavaClass().getName());
			Class<?> testClass = selector.getJavaClass();
			return remoteResolve(
				DiscoverySelectors.selectClass(testClass), 
				testClass, 
				context
			);
		}
		
		
		/**
		 * Resolution by method selector. This is invoked, when choosing to run a single method 
		 * of a test class. 
		 * We require the class of the method to be z2 junit testable. If so, we remote resolve
		 * with the method selector and leave it all to the remote resolution. 
		 */
		@Override
		public Resolution resolve(MethodSelector selector, Context context) {
			LOG.fine("Resolving method selector "+selector.getClassName()+"."+selector.getMethodName());
			if (isZ2UnitTestable.test(selector.getJavaClass())) {
				// ok, it is z2 testable. 
				// do the rest remotely.
				// We do not even need to translate the selector, as that is exactly what we look 
				// for remotely
				return remoteResolve(
					selector,
					selector.getJavaClass(),
					context
				);
			} else  {
				return Resolution.unresolved();
			}
		}
		
		/**
		 * Resolution by id selector. This is invoked, when choosing to re-run a previously discovered test
		 * and we need to tie it in with our class based discovery to reconstruct the test hierarchy and 
		 * test plan and to eventually execute a single id selected test.
		 * 
		 * NOTE: There is something tricky happening here. In presence of dynamic tests, the test plan as
		 * returned from remote during discovery will not have the dynamic tests yet. But when 
		 * executing, we are not sending our test plan but instead refer to a remotely held test plan that
		 * was previously discovered and still knows about the unique id selection and so about limitation
		 * to very specific dynamic tests. JUnit will filter out anything later dynamically registered
		 * that does not fit the discovery selection leading to the test plan.
		 */
		@Override
		public Resolution resolve(UniqueIdSelector selector, Context context) {
			if (selector.getUniqueId().getSegments().get(0).getValue().equals(Z2_JUPITER)) {
				LOG.info("Resolving Unique ID "+selector.getUniqueId());
				// it's us. 
				// second segment should be a class name, if we provided it previously
				String className = selector.getUniqueId().getSegments().get(1).getValue();
				Class<?> testClass = tryToLoadClass(className).getOrThrow(cause -> new JUnitException("Unknown class: " + className, cause));
				// determine the remote unique id to resolve for by removing the prefix
				// however, tools sometimes only send up to the first class name. In that case, we resolve by class
				if (selector.getUniqueId().getSegments().size()==2) {
					// (we must have prefixed this accordingly in a previous class based discovery)
					return remoteResolve(
						DiscoverySelectors.selectClass(testClass), 
						testClass, 
						context
					);
				} else {
					// (we must have prefixed this accordingly in a previous class based discovery)
					return remoteResolve(
						DiscoverySelectors.selectUniqueId(UniqueIds.subUniqueId(selector.getUniqueId(),2)), 
						testClass, 
						context
					);
				}
			} else {
				return Resolution.unresolved();
			}
		}

		/**
		 * Resolve a selector leading via a z2 unit test class given the context
		 * and via a remote discovery (see below) 
		 */
		private Resolution remoteResolve(DiscoverySelector select, Class<?> testClass, Context context) {
			if (!isZ2UnitTestable.test(testClass)) {
				return Resolution.unresolved();
			} else {
				// Run a remote discovery
				// Add a descriptor that has representation of all the remote descriptors
				// in its nested hierarchy.
				// NOTE that the remote descriptor includes all client test descriptors with a prefix
				Z2JupiterRemoteTestDescriptor d = context.addToParent(parent->{
					return Optional.of(
						remoteDiscover(
							testClass.getAnnotation(Z2JupiterTestable.class),
							parent.getUniqueId().append(SEGMENT_TYPE_TEST,testClass.getName()),
							Z2_JUPITER_DISPLAY_PREFIX+testClass.getSimpleName(),
							select,
							ClassSource.from(testClass)
						)
					);
				}).get();
				registerClaim(testClass);
				// exact match with our descriptor.
				return Resolution.match(Match.exact(d));
			}
		}

		/**
		 * run a remote discovery and create
		 * a remote descriptor that wraps the whole
		 * remote testplan and prefixes (by uniqueid) 
		 * all that was discovered
		 */
		private Z2JupiterRemoteTestDescriptor remoteDiscover(Z2JupiterTestable config, UniqueId prefix, String displayName, DiscoverySelector selector, TestSource source) {
			Z2JupiterClient client = new Z2JupiterClient(config);
			LauncherDiscoveryRequest dr = LauncherDiscoveryRequestBuilder.request().selectors(selector).build();
			Z2JupiterTestPlanDto tp = client.discover(new Z2JupiterLauncherDiscoveryRequestDto(dr));
			return new Z2JupiterRemoteTestDescriptor(
				prefix,
				displayName,
				source,
				Type.CONTAINER,
				client,
				tp
			);
		}
		
		/**
		 * register what we found with the filter so that
		 * the filter can later enforce our claim on the class
		 * and filter out what any other test engine thought
		 * about it.
		 */
		private void registerClaim(Class<?> testClass) {
			Z2JupiterPostDiscoveryFilter.discovered.get().add(testClass);
		}
		

	} // end resolver
	
	private static final EngineDiscoveryRequestResolver<TestDescriptor> resolver = EngineDiscoveryRequestResolver.builder()
		// all annotated containers
		.addClassContainerSelectorResolver(isZ2UnitTestable)
		.addSelectorResolver(context->new Z2UnitSelectorResolver(context))
		.build();

	@Override
	public String getId() {
		return Z2_JUPITER;
	}

	@Override
	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		Z2JupiterTestEngineDescriptor engineDescriptor = new Z2JupiterTestEngineDescriptor(uniqueId);
		if (isEnabled()) {
			resolver.resolve(discoveryRequest, engineDescriptor);
		}
		return engineDescriptor;
	}

	@Override
	public void execute(ExecutionRequest request) {
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		Z2JupiterTestEngineDescriptor engineDescriptor = (Z2JupiterTestEngineDescriptor) request.getRootTestDescriptor();
		engineExecutionListener.executionStarted(engineDescriptor);
		executeAll(engineDescriptor,engineExecutionListener, request);
		engineExecutionListener.executionFinished(engineDescriptor, successful());
	}

	/**
	 * Execution is simple. Whenever we find a {@link Z2JupiterRemoteTestDescriptor}, 
	 * we perform execution of a corresponding test plan (we retrieved during 
	 * discovery) remotely and map listener invocations to our local, nested
	 * test hierarchy
	 */
	private void executeAll(TestDescriptor d, EngineExecutionListener listener, ExecutionRequest request) {
		if (d instanceof Z2JupiterRemoteTestDescriptor) {
			// We found our remote descriptor that was discovered previously
			// now run the tests
			Z2JupiterRemoteTestDescriptor rd = (Z2JupiterRemoteTestDescriptor) d; 
			Z2JupiterEventEngineExecutionListenerAdapter adapter = new Z2JupiterEventEngineExecutionListenerAdapter(
				rd.getUniqueId(),
				listener,
				id->request.getRootTestDescriptor().findByUniqueId(id).orElse(null)
			);
			rd.getClient().execute(rd.getTestPlan().getId(), adapter::dispatch);
		} else {
			// recurse
			d.getChildren().forEach(td->executeAll(td, listener, request));
		}
	}
}
