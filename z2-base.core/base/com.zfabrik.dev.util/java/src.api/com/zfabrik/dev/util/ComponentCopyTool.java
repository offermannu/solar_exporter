/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.provider.util.FSCRDBComponent;
import com.zfabrik.util.fs.FileUtils;

public class ComponentCopyTool {

	/** 
	 * Type of copy mode. 
	 *
	 */
	public static enum Mode {
		/**
		 * Copy all resources as is
		 */
		PLAIN, 
		/**
		 * Copy binaries, remove source code folders
		 */
		BINARY, 
		/**
		 * As BINARY, but in addition remove java source files from 
		 * binaries, if present
		 */
		NOJAVA 
	}

	
	private final static DateFormat df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);

	private static final FileFilter JAVA_ONLY = new FileFilter() {
		@Override
		public boolean accept(File pathname) {
			return (!pathname.isDirectory() && pathname.getName().endsWith(".java")) || (pathname.isDirectory() && pathname.list().length==0);
		}
	};

	/**
	 * Inpect a component and create a component repository implementation that holds more meta data. In particular whether there
	 * is resources other than properties.
	 */
	public static FSCRDBComponent inspectComponent(String cn) throws IOException {
		IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(cn);
		if (d==null) {
			throw new IllegalArgumentException("Component "+cn+" not found");
		}
		FSCRDBComponent c = new FSCRDBComponent(cn);
		File cf = IComponentsManager.INSTANCE.retrieve(cn);
		boolean propsOnly = cf==null || !new File(cf,"z.properties").exists();
		c.setHasFile(propsOnly);
		c.setHasFolder(!propsOnly);
		c.setProperties(d.getProperties());
		c.setRevision(d.getRevision());
		return c;
	}
	
	
	/**
	 * Copy a component's resource folder into target. Depending on the mode setting, source files and folders may be stripped while 
	 * copying. Note that this operation may trigger a compilation step.
	 * This method expects a {@link FSCRDBComponent} object as created from {@link #inspectComponent(String)}.
	 */
	public static void copyComponentResources(String cn, final File target, Mode mode) throws Exception {
		FSCRDBComponent c = inspectComponent(cn);
		if (c.isHasFolder()) {
			int p = cn.lastIndexOf('/');
			String moduleName = cn.substring(0,p);
			
			File origin = IComponentsManager.INSTANCE.retrieve(cn);

			target.mkdir();
			
			boolean isJavaComponent = IJavaComponent.TYPE.equals(c.getType());
			File resources;
			if (isJavaComponent) {
				// force compile and get resources
				resources = IComponentsLookup.INSTANCE.lookup(cn, IJavaComponent.class).getRuntimeResources();
			} else {
				resources = origin;
			}
			
			
			// copy all but gen folders
			FileUtils.copy(resources, target, new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					return !(pathname.getName().equalsIgnoreCase("gen") && pathname.isDirectory() && pathname.getParentFile().equals(resources));
				}
			});

			
			if (!Mode.PLAIN.equals(mode) && isJavaComponent) {
				
				
				// special treatment for java components to
				// a) compensate for includes and b) strip off source
				// files
				File targetApiFolder = new File(target,"bin.api");
				File targetApiLibFolder = new File(targetApiFolder,"lib");
				File targetImplFolder = new File(target,"bin.impl");
				File targetImplLibFolder = new File(targetImplFolder,"lib");

				String apiJarName = moduleName+".api.jar";
				String implJarName = moduleName+".impl.jar";

				// remove all src stuff
				FileUtils.delete(new File(target,"src"));
				FileUtils.delete(new File(target,"src.api"));
				FileUtils.delete(new File(target,"src.impl"));
				FileUtils.delete(new File(target,"src.test"));

				// also remove all test resources
				FileUtils.delete(new File(target,"bin.test"));

				// now target has a good structure.
				// remove source files from jars
				if (Mode.NOJAVA.equals(mode)) {
					// process api jar
					strip(new File(targetApiLibFolder,apiJarName));
				}
				if (Mode.NOJAVA.equals(mode)) {
					// process impl jar
					strip(new File(targetImplLibFolder,implJarName));
				}
			}
		}
	}
	
	/**
	 * Create a component's property file, if the component has only properties and no resource folder  
	 */
	public static void writeProperties(String cn, File targetFolder) throws Exception {
		FSCRDBComponent c = inspectComponent(cn);
		int p = cn.lastIndexOf('/');
		String componentName = cn.substring(p+1);
		// only write properties. Clean out some generated stuff first
		Properties pr = new Properties();
		pr.putAll(c.getProperties());
		pr.remove(IComponentsManager.COMPONENT_REPO_IMPLEMENTATION);
		File pf = new File(targetFolder,componentName+".properties");
		// write 
		OutputStream fout = new FileOutputStream(pf);
		try {
			pr.store(fout, "Exported "+df.format(new Date(System.currentTimeMillis())));
		} finally {
			fout.close();
		}
	}
	
	
	/**
	 * Copy a component into a file system component resource format. That is, the component layout will be rebuild in the target folder,
	 * as if to be fed into another standard component repository.
	 * I.e. if the component has properties only, a file
	 * <p>
	 * &lt;module&gt;/&lt;component&gt;.properties
	 * </p>
	 * will be created, and if the component has resources, the complete folder structure
	 * <p>
	 * &lt;module&gt;/&lt;component&gt;
	 * </p>
	 * will be created. Depending on the mode setting, source files and folders may be stripped while 
	 * copying. Note that this operation may trigger a Java compilation.
	 *  
	 * @param cn
	 * @param target
	 * @param mode
	 * @return
	 * @throws Exception
	 */
	public static void copyComponentToRepositoryFolder(String cn, File target, Mode mode) throws Exception {
		int p = cn.lastIndexOf('/');
		String moduleName = cn.substring(0,p);
		String componentName = cn.substring(p+1);

		FSCRDBComponent c = inspectComponent(cn);
		// create the module
		File moduleFolder = new File(target,moduleName);
		moduleFolder.mkdirs();
		
		if (c.isHasFile()) {
			// props
			writeProperties(cn, moduleFolder);
		} 
		if (c.isHasFolder()){
			// all resources
			copyComponentResources(cn, new File(moduleFolder,componentName), mode);
		}
	}

	// 
	// strip all java sources from file
	private static void strip(File file) throws IOException {
		if (file.exists()) {
			String n = file.getName();
			n = n.substring(0,n.length()-4);
			File tf = new File(file.getParentFile(),n+".gen");
			tf.mkdir();
			FileUtils.unzip(file, tf);
			delete(tf,JAVA_ONLY);
			if (tf.exists()) {
				FileUtils.zip(tf,new File(file.getParentFile(),n+"_.jar"));
				FileUtils.delete(tf);
			}
			FileUtils.delete(file);
		}
	}

	private static void delete(File tf, FileFilter filter) {
		if (tf.isDirectory()) {
			File[] all = tf.listFiles();
			for (File f : all) {
				delete(f,filter);
			}
		}
		if (filter.accept(tf)) {
			tf.delete();
		}
	}

}
