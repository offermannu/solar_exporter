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
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class SyncFileStructure {

	private final static Logger logger = Logger.getLogger(SyncFileStructure.class.getName());
	
	private final File rootDir;
	private final FileFilter filter;
	
	/**
	 * <code>rootDir</code> is created if it does not exist yet
	 *  
	 * @param rootDir root directory of the target structure
	 * @param filter filter for files which are ignored
	 * 
	 * @throws NullPointerException if <code>rootDir</code> or <code>sourceFiles</code>
	 */
	public SyncFileStructure(File rootDir, FileFilter filter) {
		if (rootDir == null) throw new NullPointerException("rootDir must not be null");
		if (! rootDir.exists()) {
			// create rootDir if it does not exist yet
			if (! rootDir.mkdir()) throw new IllegalStateException("Failed to create root dir: " + rootDir);
		}
		if (!rootDir.canRead() && !rootDir.canWrite()) {
			throw new IllegalStateException("No Permissions for rootdir: " + rootDir);
		}

		this.rootDir = rootDir;
		this.filter = filter != null? filter : new FileFilter() {
			
			public boolean accept(File pathname) {
				return true;
			}
		};
	}
	
	public SyncFileStructure() {
		this(new File("."), null);
	}
	
	/**
	 * "Applies" the files given as file names in <code>sourceFiles</code> to the given <code>rootDir</code>.
	 * That means it changes the content of the root folder so that it will look like the given file names.
	 * This is achieved by
	 * <ol>
	 *  <li>ignoring all existing directories and files which are listed in <code>sourcefiles</code> 
	 * 	<li>adding all directories and files which do not yet exist in the target structure (files are created with size=0)
	 *  <li>deleting all directories and files which exist in the target structure but are not listed in the source files
	 * </ol>
	 * 
	 * The file names are interpreted as relative pathnames to the root directory. A file name which ends with
	 * a slash is interpreted as directory otherwise as a file. 
	 * The file name list must contain all directories explicitly, that means if it contains an entry a/b/c.txt, 
	 * it must contain the entries a/ and a/b/ as well.
	 * 
	 * @param sourceFiles list of file names which are applied to the target structure
	 * 
	 * @throws IllegalStateException if some files in the target structure cannot be written or deleted
	 * @throws IOException if there are other problems while creating or deleting files and directories
	 * 
	 */
	public void apply(List<String> sourceFiles) throws IOException {
		
		if (this.rootDir == null) throw new NullPointerException("rootDir must not be null");
		if (sourceFiles == null) throw new NullPointerException("sourcefiles must not be null");
		if (! this.rootDir.exists()) {
			// create rootDir if it does not exist yet
			if (! this.rootDir.mkdir()) throw new IllegalStateException("Failed to create root dir: " + this.rootDir);
		}
		if (!this.rootDir.canRead() && !this.rootDir.canWrite()) {
			throw new IllegalStateException("No Permissions for rootdir: " + this.rootDir);
		}
		
		// retrieve the (sorted) content of the target structure
		List<File> sortedTargetFiles = getAllSubFiles();
		Collections.sort(sortedTargetFiles);
				
		// sort source files and target files, so they can be compared pair by pair within one iteration
		List<String> sortedSourceFiles = new ArrayList<String>(sourceFiles);
		Collections.sort(sortedSourceFiles);
				
		Iterator<String> loopSrcFiles = sortedSourceFiles.iterator();
		Iterator<File> loopTrgFiles = sortedTargetFiles.iterator();

		String srcName = null;
		File srcFile = null;
		File trgFile = null;

		while (loopSrcFiles.hasNext() || loopTrgFiles.hasNext()) {
			
			// CASE 1: no more source files: remove all remaining target files & dirs 
			if (! loopSrcFiles.hasNext()) {
				logger.info("CASE 1");
				delete(loopTrgFiles);
				break;
			}
			
			// CASE 2: no more target files: create all remaining source files & dirs 
			if (! loopTrgFiles.hasNext()) {
				logger.info("CASE 2");
				create(this.rootDir, loopSrcFiles);
				break;
			}
			
			// CASE 3-5: have source and target  

			if (srcName == null) {
				srcName = loopSrcFiles.next();
				srcFile = new File(this.rootDir, srcName);
			}
			if (trgFile == null) {
				trgFile = loopTrgFiles.next();
			}
			
			int comp = srcFile.compareTo(trgFile);
			if (comp == 0) {
				// CASE 3: there's a matching target file for a source file: IGNORE this pair and move on
				logger.info("CASE 3 '" + srcFile + "' = '" + trgFile + "'");
				srcName = null; 
				srcFile = null;
				trgFile = null;
				
			} else if (comp < 0) {
				// CASE 4: no matching target file for current source file: create this source file
				logger.info("CASE 4 '" + srcFile + "' < '" + trgFile + "'");
				create(this.rootDir, srcName);
				
				// move on with next source file but keep target file
				srcName = null; 
				srcFile = null;
				
			} else {
				// CASE 5: no matching source file for current target file: delete this target file
				logger.info("CASE 5 '" + srcFile + "' > '" + trgFile + "'");
				delete(trgFile);
				
				// move on with next target file but keep source file
				trgFile = null;
			}
		}
	}
	
	public List<String> listDirectory() {
		return listDirectory(this.rootDir);
	}
	
	public List<String> listDirectory(File root) {
		final List<File> files = getAllSubFiles(root, new ArrayList<File>());
		final int numSkip = root.getAbsolutePath().length();
		return new AbstractList<String>() {

			@Override
			public String get(int index) {
				File f = files.get(index);
				String result = f.getAbsolutePath().substring(numSkip);
				if (f.isDirectory()) result += "/";
				return result;
			}

			@Override
			public int size() {
				return files.size();
			}
		};
		
	}
	

	private List<File> getAllSubFiles() {
		List<File> result = getAllSubFiles(this.rootDir, new ArrayList<File>());
		return result;
	}

	private List<File> getAllSubFiles(File rootDir, final List<File> result) {
		
		File[] files = rootDir.listFiles(new FileFilter() {
			
			public boolean accept(File f) {
				
				if (filter.accept(f)) {
					if (f.canRead() && f.canWrite()) {
						return true;
					} else {
						throw new IllegalStateException("No Permissions for " + f);
					}
				} else {
					return false;
				}
			}	 
		});

		for (File f : files) {
			result.add(f);
			if (f.isDirectory()) {
				getAllSubFiles(f, result);
			}
		}
		
		return result;
	}

	private void create(File rootDir, Iterator<String> fileNames) throws IOException {
		while(fileNames.hasNext()) {
			String fName = fileNames.next();
			create(rootDir, fName);
		}
	}
	
	private void create(File rootDir, String fName) throws IOException {
		File newFile = new File(rootDir, fName);
		if (newFile.exists()) {
			logger.warning(fName + " already exists in " + rootDir.getAbsolutePath());
			
		} else if (fName.endsWith("/")) {
			// its a directory
			if (! newFile.mkdir()) {
				logger.warning("Failed to create directory " + fName + " in " + rootDir);
			}
		} else {
			// its a file
			if (! newFile.createNewFile()) {
				logger.warning("Failed to create file " + fName + " in " + rootDir);
			}
		}
	}
	

	private void delete(Iterator<File> files) {
		while (files.hasNext()) {
			delete(files.next());
		}
	}
	private void delete(File f) {

		if (! f.exists()) return;
		
		if (f.isDirectory()) {
			File[] problemFiles = f.listFiles(new FileFilter() {
				
				public boolean accept(File f) {
					if (f.isDirectory()) {
						// delete directory content before
						delete(f);
					}
					// add file to list files if deletion fails
					return ! f.delete();
				}
			});
		
			if (problemFiles.length > 0) {
				logger.severe("Failed to delete the following files: " + problemFiles.toString());
			}
		}
			
		if (! f.delete()) {
			logger.severe("Failed to delete: " + f);
		}
	}
}