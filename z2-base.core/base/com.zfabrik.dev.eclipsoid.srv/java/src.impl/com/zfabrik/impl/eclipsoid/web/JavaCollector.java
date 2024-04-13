/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.web;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class JavaCollector {

	private File rootDir;
	private List<File> fileList;

	public JavaCollector(File rootDir) {
		setRootDir(rootDir);
		reset();
	}

	public void setRootDir(File rootDir) {
		assert rootDir != null;
		assert rootDir.exists();
		assert rootDir.isDirectory();
		assert rootDir.canRead();

		this.rootDir = rootDir;
	}

	public void reset() {
		this.fileList = new ArrayList<File>();
	}

	public List<File>  scan() {
		scan(this.rootDir);
		return fileList;
	}

	private void scan(File aDir) {

		File[] filesArr = aDir.listFiles(new FileFilter() {
			
			public boolean accept(File pathname) {
				return 
					pathname.exists() && 
					pathname.canRead() && 
					pathname.isFile();
			}
		});
		
		if (filesArr != null) {
			for (File f : filesArr) {
				fileList.add(f);
			}
		}

		// find all sub-directories
		File[] subDirs = aDir.listFiles(new FileFilter() {

			public boolean accept(File pathname) {
				return 
					pathname.exists() && 
					pathname.canRead() && 
					pathname.isDirectory();
			}
		});

		if (subDirs != null) {
			for (File subD : subDirs) {
				scan(subD);
			}
		}
	}
}
