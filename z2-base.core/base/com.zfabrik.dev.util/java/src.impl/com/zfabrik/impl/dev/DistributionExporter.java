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

import static com.zfabrik.dev.util.ComponentCopyTool.copyComponentToRepositoryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.dev.util.ComponentCopyTool;
import com.zfabrik.dev.util.ComponentCopyTool.Mode;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.expression.X;
/**
 * Main program (typically launched via the MainRunner that exports
 * a whole z2 repository distribution into a file system repository layout into a given
 * folder.
 * <p>
 * If desired the exported repository distribution contains no Java sources but only
 * compilation artifacts, i.e. jar files and the like.
 * <p> 
 * Syntax is: &lt;options&gt;  
 * <p>
 * Options:
 * <dl>
 * <dt>-out &lt;folder&gt;</dt>
 * <dd>Specification of output folder to store the repository layout. This folder should not exist or be empty</dd>
 * <dt>-mode {plain|binary|nojava}</dt>
 * <dd>If set to binary, only export binaries and remove all source artifacts. If set to nojava, export binaries corresponing to Java sources and remove all Java sources, but do not remove other source artifacts. Default is <code>plain</code>.</dd>
 * </dl>
 * 
 * @author hb
 *
 */
public class DistributionExporter extends Resource{
	private final static String OUT="-out";
	private final static String MODE="-mode";
	
	private static Mode mode = ComponentCopyTool.Mode.PLAIN;
	
	
	@Override
	public <T> T as(Class<T> clz) {
		if (Class.class.equals(clz)) {
			return clz.cast(DistributionExporter.class);
		}
		return null;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File out = null;
		
		//
		// check params
		//
		for (int i=0; i<args.length; i++) {
			if (OUT.equals(args[i])) {
				i++;
				if (i<args.length) {
					out =new File(args[i].trim());
				} else {
					throw new IllegalArgumentException("expected out folder specification");
				}
			} else
			if (MODE.equals(args[i])) {
				i++;
				if (i<args.length) {
					try {
						mode = Mode.valueOf(args[i].toUpperCase());
					} catch (IllegalArgumentException iae) {
						throw new IllegalArgumentException("expected one of "+Arrays.asList(Mode.values()));
					}
				} else {
					throw new IllegalArgumentException("expected one of "+Arrays.asList(Mode.values()));
				}
			}
		}
		
		if (out==null) {
			throw new IllegalArgumentException("Please specify an output folder (-out <folder>)");
		}
		
		// 
		// go!!
		//
		
		if (out.exists() && out.list().length!=0) {
			throw new RuntimeException("Output folder already exists and is not empty!");
		}
		out.mkdirs();
		//
		Collection<String> all = IComponentsManager.INSTANCE.findComponents(X.val(true));
		
		for (String cn : all) {
			IComponentDescriptor d = IComponentsManager.INSTANCE.getComponent(cn);
			if (d.getProperty(IComponentsManager.COMPONENT_REPO_IMPLEMENTATION)!=null) {
				// it's not from the local core repo. So export
				logger.info("Exporting: "+cn);
				copyComponentToRepositoryFolder(cn, out, mode);
			}
		}

	}

	private final static Logger logger = Logger.getLogger(DistributionExporter.class.getName());
}
