/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.javadoc;

import static com.zfabrik.impl.javadoc.ComponentJavaDoc.JavaDocType.API;
import static com.zfabrik.impl.javadoc.ComponentJavaDoc.JavaDocType.IMPL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.tools.DocumentationTool;
import javax.tools.ToolProvider;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.components.java.LangLevel;
import com.zfabrik.components.provider.util.LockingRevFile;
import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.html.Escaper;

/**
 * A simple javadoc supply tool that attempts to invoke the javadoc generation internally 
 * for z2 hosted Java components. Docs are provided directly into the generated java component
 * resources. 
 */
public class ComponentJavaDoc {
	
	public static enum JavaDocType {
		API, IMPL;

		public static JavaDocType parseType(String type) {
			if (type!=null) {
				try {
					return JavaDocType.valueOf(type.toUpperCase());
				} catch (Exception e) {
					// ignore
				}
			}
			return null;
		}
	};
	
	private static String getJavaDocURL(LangLevel ll) {
		return "https://docs.oracle.com/en/java/javase/"+ll.toString()+"/docs/api/";
	}
	
	private static final FileFilter FOLDER_OR_JAVA_AND_NOT_HIDDEN = new FileFilter() {
		public boolean accept(File pathname) {
			return (pathname.isDirectory() || pathname.getName().endsWith(".java")) && !pathname.getName().startsWith(".");
		}
	};
	
	private static final String IMPLEMENTATION_VERSION = LangLevel.HIGHEST.name();
		
	private JavaDocType type;
	private String component;
	private boolean resolved;
	private File root;
	
	public ComponentJavaDoc(String component,JavaDocType type) {
		super();
		this.type = type;
		this.component = component;
	}
	
	
	public File getRootFolder(boolean create) {
		this.resolve(create);
		return this.root;
	}
	
	public List<File> getSrcFolders() {
		try {
			IJavaComponent jc = IComponentsLookup.INSTANCE.lookup(component, IJavaComponent.class);
			if (jc!=null) {
				File croot = IComponentsManager.INSTANCE.retrieve(component);
				if (croot.exists()) {
					return getSrcFoldersInComponentFolder(croot, this.type);
				}
			}
			return Collections.emptyList();
		} catch (IOException e) {
			throw new IllegalStateException("Component retrieval failed",e);
		}
	}

	
	private File resolve(boolean create) {
		if (!this.resolved) {
			this.root = getJavaDoc(this.component,this.type, create);
			this.resolved = true;
		}
		return this.root;
	}
	
	private static File getJavaDocFolderInComponentFolder(File cf, JavaDocType type) {
		return (type==JavaDocType.IMPL ? new File(cf,"gen/doc.impl"):new File(cf,"gen/doc.api"));
	}
	
	private static List<File> getSrcFoldersInComponentFolder(File cf, JavaDocType type) {
		return (type==JavaDocType.IMPL? Collections.singletonList(new File(cf,"src.impl")):Arrays.asList(new File(cf,"src.api"),new File(cf,"src")));
	}
	
	private static synchronized File getJavaDoc(String component, JavaDocType type, boolean create)  {
		try {
			// need to check.
			// rely on the java components logic. Look it up as Java Component to
			// force compilation check
			IJavaComponent jc = IComponentsLookup.INSTANCE.lookup(component, IJavaComponent.class);
			if (jc!=null) {
				IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(component);
				File croot = IComponentsManager.INSTANCE.retrieve(component);
				File jdoc  = getJavaDocFolderInComponentFolder(croot, type);
				if (Boolean.parseBoolean(desc.getProperty(IJavaComponent.NOBUILD))) {
					// no jdoc gen for no build components
					if (jdoc.exists()) {
						return jdoc;
					}
					return null;
				} else {
					// check revision
					String revfile = "doc_"+(type==IMPL?"impl":"api")+".rev";
					LockingRevFile lrf = new LockingRevFile(new File(croot,revfile));
					lrf.open();
					LangLevel langLevel = LangLevel.determine();
					try {
						if (jdoc.exists() && langLevel.name().equals(lrf.properties().get("v"))) {
							// javadocs exist, no build component or right version 
							return jdoc;
						} else {
							if (!create) {
								// just do, as if not there
								return null;
							}
							logger.info("Updating Javadoc for component "+component+" ("+(type==IMPL?"impl":"api")+")");
							// no jdocs or wrong implementation version
							// must generate javadocs
							// check javadoc tool
							// compute params
							List<String> params = new LinkedList<String>();
							List<File> files = new LinkedList<File>();
							
							// 1. source paths and packages
							List<File> fs = new LinkedList<File>();
							if (type==IMPL) {
								File sf = new File(croot,"src.impl");
								if (sf.exists()) {
									fs.add(sf);
								}
							} else {
								File sf = new File(croot,"src.api");
								if (sf.exists()) {
									fs.add(sf);
								}
								sf = new File(croot,"src");
								if (sf.exists()) {
									fs.add(sf);
								}
							}
			
							if (!fs.isEmpty()) {
								// 1a. ok, there is something to do
								StringBuilder sourcepath = new StringBuilder(200);
								for (File f : fs) {
									// add as source path and
									// list all packages
									int l = files.size();
									addSourceFiles(files,f);
									if (l<files.size()) {
										sourcepath.append((sourcepath.length()>0? File.pathSeparator:"")).append(f.getCanonicalPath());
									}
								}
								params.add("-sourcepath");
								params.add(sourcepath.toString());
								
								// 2. deps translate to -linkoffline relations
								addLinks(component, new HashSet<String>(), params, type, true);							

								// 3. link to the JDK
								params.add("-link");
								boolean docLintNone = true;
								
								params.add(getJavaDocURL(langLevel));
								
								// 3. class path
								StringBuilder classpath = new StringBuilder(200);
								// add the classpath
								addToPath(classpath, JavaComponentUtil.getJavaComponent("com.zfabrik.core.api").as(IJavaComponent.class).getPublicLoader().getURLs());
								addToPath(classpath, (type==IMPL? jc.getPrivateLoader().getURLs() : jc.getPublicLoader().getURLs()));

								params.add("-classpath");
								params.add(classpath.toString());
								
								// 4. outpath
								FileUtils.delete(jdoc);
								jdoc.mkdirs();
								params.add("-d");
								params.add(jdoc.getCanonicalPath());
								
								if (docLintNone) {
									// relax!
									params.add("-Xdoclint:none");
								}
								
								if (!files.isEmpty()) {
									// 5. add files to generate javadoc for
									File fileList = File.createTempFile("javadoc_",".lst");
									BufferedWriter w = new BufferedWriter(new FileWriter(fileList));
									try {
										for (File sf : files) {
											w.write(sf.getAbsolutePath());
											w.newLine();
										}
									} finally {
										w.close();
									}
									params.add("@"+fileList.getAbsolutePath());
									
									try {
										// 6. and GO!
										logger.info("Running Javadoc command with parameters: "+params);
										int rc;
										DocumentationTool javadoc = ToolProvider.getSystemDocumentationTool();
										if (javadoc!=null) {
											// try internal
											rc = javadoc.run(null,null,null, params.toArray(new String[params.size()]));
										} else {
											// note that we didn't get it from the JDK for info
											logger.info("No internal Javadoc tool found (are you running a JDK?) - falling back to command line");
											// run as child process
											rc = runAsChildProcess(params);
										}
										if (rc==0 && !empty(jdoc)) {
											logger.info("Javadoc command completed with exit code 0");
											// done
											lrf.properties().setProperty("v", IMPLEMENTATION_VERSION);
											lrf.update();
											return jdoc;
										} else {
											throw new RuntimeException("Javadoc command exited with code "+rc);
										}
									} finally {
										fileList.delete();
									}
								} else {
									logger.info("Found no packages: Nothing to do");
								}
							} 
						}
					} finally {
						lrf.close();
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Javadoc generation failed",e);
		}
		return null;
	}


	private static int runAsChildProcess(List<String> params) throws IOException, InterruptedException {
		params.add(0, "javadoc");
		ProcessBuilder pb = new ProcessBuilder(params);
		Process p = pb.start();
		// read streams
		handleStreams(p);										
		// wait for termination
		int rc = p.waitFor();
		return rc;
	}

	private static void handleStreams(Process p) {
		new Thread(streamReader(p.getInputStream(), "out")).start();
		new Thread(streamReader(p.getErrorStream(), "err")).start();
	}

	private static Runnable streamReader(InputStream in, String name) {
		return ()->{
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
				String l;
				while ((l=reader.readLine())!=null) {
					logger.info(name+": "+l);
				}
			} catch (IOException ioe) {
				logger.log(Level.WARNING,"Error reading javadoc command stream "+name,ioe);
			}
		};
	}

	private static void addToPath(StringBuilder classpath, URL[] urls) throws IOException {
		if (urls!=null) {
			for (URL u : urls) {
				if (classpath.length()>0) {
					classpath.append(File.pathSeparator);
				}
				classpath.append(new File(u.getPath()).getCanonicalPath());
			}
		}
	}

	private static boolean empty(File tf) {
		return (!tf.isDirectory() || tf.list().length==0);
	}


	//
	// recursively find package names in a folder hierarchy
	//
	private static void addSourceFiles(List<File> files, File f) {
		for (File g : f.listFiles(FOLDER_OR_JAVA_AND_NOT_HIDDEN)) {
			if (g.isDirectory()) {
				addSourceFiles(files,g);
			} else {
				files.add(g);
			}
		}
	}

	//
	// add external refs as link params following the 
	// java refs
	//
	private static void addLinks(String component, Set<String> traversed, List<String> params, JavaDocType type, boolean start) throws IOException {
		component = JavaComponentUtil.fixJavaComponentName(component);
		if (traversed.contains(component+"@"+type)) {
			// already traversed.
			return;
		}
		traversed.add(component+"@"+type);
		
		IComponentDescriptor desc = IComponentsManager.INSTANCE.getComponent(component);
		if (desc==null) {
			return;
		}
		
		// deps 
		addAllLinks(traversed, params, desc.getProperty(IJavaComponent.PUBREFS));
		if (start && type==IMPL) {
			// add private references
			addAllLinks(traversed, params, desc.getProperty(IJavaComponent.PRIREFS));
			// add our own API
			addLinks(component, traversed, params, API,false);
		}
		
		if (start) {
			// at start of recursion add the always present core api
			// add the core api
			addLinks("com.zfabrik.core.api",traversed,params,API,false);
		} else {
			// otherwise add the actual API
			File f = getJavaDoc(component, API, true);
			if (f!=null) {
				params.add("-linkoffline");
				params.add("/javadoc/"+Escaper.urlEncode(component,'!')+"/api");
				params.add(f.toURI().toString());
			}
		}
	}

	private static void addAllLinks(Set<String> traversed, List<String> params,String refs) throws IOException {
		if (refs!=null) {
			StringTokenizer tk = new StringTokenizer(refs);
			while (tk.hasMoreTokens()) {
				addLinks(tk.nextToken().trim(), traversed, params, API,false);
			}
		}
	}

	private final static Logger logger = Logger.getLogger(ComponentJavaDoc.class.getName());
}
