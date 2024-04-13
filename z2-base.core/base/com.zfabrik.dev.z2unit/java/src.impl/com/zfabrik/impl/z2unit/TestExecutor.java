/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.z2unit;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.RunListener;
import org.junit.runners.BlockJUnit4ClassRunner;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.z2unit.annotations.Z2UnitTest;

/**
 * Manages the actual execution of a z2-side unit test. Given a class name, component name, and a test description,
 * this class plays a test invocation as if locally but initiated from remote.
 * In particular filtering and sorting are moved from remote to here.
 * All events are passed on to a run listener that in turn streams them back to remote.
 */

public class TestExecutor {
	private String componentName;
	private String className;
	private JUnitCore junitCore;
	private Description description;
	private ClassLoader classLoader;
	
	private Class<?> testClass;
	
	public TestExecutor(String componentName, String className) {
		super();
		this.componentName = componentName;
		this.className = className;
		
		// prepare
		this.junitCore = new JUnitCore();
		
	}

	public ClassLoader getClassLoader() {
		if (this.classLoader==null) {
			IJavaComponent jc = JavaComponentUtil.getJavaComponent(componentName).as(IJavaComponent.class);
			if (jc==null) {
				throw new IllegalArgumentException("Found no Java component for component name "+componentName);
			}
			this.classLoader = jc.getPrivateLoader();
		}
		return classLoader;
	}
	
	public Class<?> getTestClass() {
		if (this.testClass==null) {
			Class<?> clz = null;
			try {
				clz=Class.forName(this.className, false, getClassLoader());
			} catch (ClassNotFoundException cnfe) {
				throw new RuntimeException("Class "+this.className+" not found for component name "+this.componentName);
			} catch (Exception e) {
				throw new RuntimeException("Failed to load class "+this.className+" for component name "+this.componentName,e);
			}
			this.testClass = clz;
		}
		return testClass;
	}
	
	public void setDescription(Description description) {
		this.description = description;
	}
	
	public void addRunListener(RunListener runListener) {
		this.junitCore.addListener(runListener);
	}
	
	public void run() {
		assert this.description!=null;
		
		Class<?> clz = getTestClass();
		
		Set<IDependencyComponent> dependencies = new HashSet<IDependencyComponent>();
		Request r = null;

		// set up the test request
		try {
			Z2UnitTest z2u = clz.getAnnotation(Z2UnitTest.class);
			if (z2u!=null) {
				// apply configuration
				// check for a custom runner
				if  (z2u.runWith()!=Runner.class) {
					// use custom runner
					r = Request.runner(z2u.runWith().getConstructor(Class.class).newInstance(clz));
				}
				// check for prepare dependencies
				if (z2u.dependencies()!=null) {
					for (String cn : z2u.dependencies()) {
						IDependencyComponent dc = IComponentsLookup.INSTANCE.lookup(cn, IDependencyComponent.class);
						if (dc!=null) {
							// house keeping
							dependencies.add(dc);
							dc.prepare();
						}
					}
				}
			}
			
			if (r==null) {
				// use the default. It is important to specify the runner explicitly here, 
				// as otherwise the z2 unit test runner would be picked up again and we would 
				// trigger a bad case of recursion.
				r = Request.runner(new BlockJUnit4ClassRunner(clz));
			}
			
			
			//
			// Now we have a runner. From the client, we retrieved a test description. This mirrors 
			// filtering and sorting by the IDE for example (or the runner).
			//
			// Here, serving side, we filter and sort again based on that description - as a best-effort approach to replay the result.
			// The intrinsic problem here is that the original filter (IDE) is not accessible anymore and runners may be context sensitive.
			//
			//
			// As supporting structure we build a traversal map for filtering and sorting
			Map<Description, Integer> order = new HashMap<>();
			traverse(order, 0, description);
			
			
			// filter by description
			r = r.filterWith(new Filter() {
				@Override
				public boolean shouldRun(Description d) {
					return order.containsKey(d);
				}
				
				@Override
				public String describe() {
					return description.toString();
				}
			});

			// sort by description
			r = r.sortWith(new Comparator<Description>() {
				public int compare(Description d1, Description d2) {
					Integer i1 = order.get(d1);
					Integer i2 = order.get(d2);
					if (i1==null) {
						i1 = 0;
					}
					if (i2==null) {
						i2 = 0;
					}
					return i1-i2;
				}
			});

		} catch (Exception e) {
			throw new RuntimeException("Failed to initiate test ("+this.componentName+","+this.className+")",e);
		}
		// go and run it.
		this.junitCore.run(r);
	}
	
	private int traverse(Map<Description,Integer> m, int index, Description current) {
		m.put(current, index++);
		if (current.getChildren()!=null && !current.getChildren().isEmpty()) {
			for (Description d : current.getChildren()) {
				index=traverse(m,index,d)+1;
			}
		}
		return index;
	}



}
