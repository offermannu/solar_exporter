/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.dev;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.runtime.Foundation;

/**
 * Main program (typically launched via the MainRunner that retrieves 
 * all jars from a list of java components, including their dependencies.
 * <p>
 * Syntax is: &lt;options&gt; &lt;component 1&gt; &lt;component 2&gt; ... 
 * <p>
 * Options:
 * <dl>
 * <dt>-out &lt;folder&gt;</dt>
 * <dd>Specification of output folder to store all retrieved binaries into (default to the current working folder)</dd>
 * <dt>-refs {true|false}</dt>
 * <dd>If set, follow references (default), otherwise do not follow references</dd>
 * <dt>-coreapi {true|false}</dt>
 * <dd>If set, include the z2 environment core api, otherwise do not (default)</dd>
 * </dl>
 * 
 * @author hb
 * @see MainRunner 
 */
public class JarRetriever extends Resource {
	private final static String OUT="-out";
	private final static String REFS="-refs";
	private final static String COREAPI="-coreapi";
	
	private static boolean devmode = Foundation.MODE_DEVELOPMENT.equals(System.getProperty(Foundation.MODE));
	
	public <T> T as(Class<T> clz) {
		if (Class.class.equals(clz)) {
			return clz.cast(JarRetriever.class);
		}
		return null;
	}

	public static void main(String[] args) throws Exception {
		// parse args
		String outFolder = null;
		List<String> components = new LinkedList<String>();

		boolean refs=true, coreapi=false;
		
		for (int i=0; i<args.length; i++) {
			if (OUT.equals(args[i])) {
				i++;
				if (i<args.length) {
					outFolder=args[i].trim();
				} else {
					throw new IllegalArgumentException("expected out folder specification");
				}
			} else
			if (REFS.equals(args[i])) {
				refs = bool(REFS,args,++i);
			} else
			if (COREAPI.equals(args[i])) {
				coreapi=bool(COREAPI,args,++i);
			} else {
				components.add(args[i].trim());
			}
		}
		// out folder
		File out = (outFolder==null? new File(".") : new File(outFolder));
		if (!out.exists()) {
			out.mkdirs();
		}

		if (coreapi) {
			// core apis
			components.add("com.zfabrik.core.api/java");
		}

		System.out.println("Z2 Jar Retriever resolving "+components+" (refs="+refs+", coreapi="+coreapi+")");
		
		Set<String> cmps = new HashSet<String>();
		// resolve
		Set<File> files = new HashSet<File>();
		for (String cn : components) {
			resolve(cn,cmps,files,true,refs);
		}
		// copy
		for (File f : files) {
			File t = new File(out,f.getName());
			FileUtils.copy(f,t,null);
		}
		System.out.println("Copied "+files.size()+" files from "+cmps.size()+" components.");
		// done!
	}

	private static boolean bool(String param, String[] args, int i) {
		if (i<args.length) {
			return Boolean.parseBoolean(args[i]);
		} else {
			throw new IllegalArgumentException("Expected \"true\" or \"false\" for boolean parameter "+param);
		}
	}

	private static void resolve(String cn, Set<String> cmps, Set<File> files, boolean inclImpl, boolean recurse) throws Exception {
		cn = JavaComponentUtil.fixJavaComponentName(cn);
		IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(cn);
		if (desc!=null) {
			
			// trigger build check and find resources
			IJavaComponent jc = IComponentsLookup.INSTANCE.lookup(cn,IJavaComponent.class);
			if (jc!=null) {
				File cf = jc.getRuntimeResources();
				if (cf!=null) {
					addAll(files,new File(cf,"bin/lib"));
					addAll(files,new File(cf,"bin.api/lib"));
					if (inclImpl) {
						addAll(files,new File(cf,"private/bin/lib"));
						addAll(files,new File(cf,"bin.impl/lib"));
						if (devmode) {
							addAll(files,new File(cf,"bin.test/lib"));
						}
					}
				}
				// follow the refs
				if (recurse) {
					if (!cmps.contains(cn+"@api")) {
						cmps.add(cn+"@api");
						resolveAll(desc.getProperty("java.publicReferences"),cmps,files);
					}
					if (!cmps.contains(cn+"@impl")) {
						cmps.add(cn+"@impl");
						resolveAll(desc.getProperty("java.privateReferences"),cmps,files);
					}
					if (devmode) {
						if (!cmps.contains(cn+"@test")) {
							cmps.add(cn+"@test");
							resolveAll(desc.getProperty("java.testReferences"),cmps,files);
						}
					}
				}
			} else {
				throw new IllegalArgumentException("Not a Java component: "+cn);
			}
		}
	}
	

	private static void addAll(Set<File> files, File f) {
		if (f.exists()) {
			files.addAll(Arrays.asList(f.listFiles()));
		}
	}

	private static void resolveAll(String csl, Set<String> cmps,Set<File> files) throws Exception {
		if (csl!=null) {
			StringTokenizer tk = new StringTokenizer(csl,",");
			while (tk.hasMoreTokens()) {
				String cn = tk.nextToken().trim();
				resolve(cn, cmps,files, false,true);
			}
		}
	}
}
