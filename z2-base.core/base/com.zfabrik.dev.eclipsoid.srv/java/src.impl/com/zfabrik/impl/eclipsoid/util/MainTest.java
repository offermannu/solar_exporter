/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.util;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

public class MainTest {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		SyncFileStructure sfs = new SyncFileStructure(new File("../dest"), new FileFilter() {
			
			public boolean accept(File pathname) {
				return ! pathname.getName().startsWith(".svn");
			}
		});
		
		List<String> files = sfs.listDirectory(new File("../com.zfabrik.dev.eclipsoid.srv"));
		for (String f : files) {
			System.out.println(f);
		}
		
		sfs.apply(files);
	}

}
