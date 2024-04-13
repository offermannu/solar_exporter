/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.devcr;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.provider.util.AbstractComponentRepository;
import com.zfabrik.components.provider.util.FSCRDBComponent;
import com.zfabrik.dev.local.DevRepository;
import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.runtime.Foundation;

public class DevRepoImpl extends AbstractComponentRepository<DevRepoDB> {
	private static final int PRIO = 750; // higher than SVN, less than local
//	private final static DateFormat DF = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);

	public final static String PROP_PROJECT = "com.zfabrik.dev.project";
	private static final String Z_PROPERTIES = "z.properties";
	private static final String PROPERTIES = ".properties";
	private final static FileFilter NON_HIDDEN_FOLDERS = new FileFilter(){
		public boolean accept(File pathname) {
			return ((pathname.isDirectory()) && (!pathname.getName().startsWith(".")));
		}
	};

	private final static FileFilter NON_HIDDEN_FILES = new FileFilter(){
		public boolean accept(File pathname) {
			return (!pathname.getName().startsWith("."));
		}
	};

	private List<File> wsFolders = new LinkedList<File>();
	private File temp;
	// default
	private int depth = 3;

	public DevRepoImpl(String name) {
		super(name, DevRepoDB.class);

		String wss = Foundation.getProperties().getProperty(DevRepository.LOCAL_WORKSPACE);
		if (wss==null || wss.trim().length() == 0) {
			logger.info("Dev Repository: No Workspace configuration ("+DevRepository.LOCAL_WORKSPACE+") found. Dev Repository will not be registered");
			return;
		}

		// configure
		configure(PRIO,null,name);

		// set depth - either from our component props or from system props - otherwise go by the default
		String dps = IComponentsManager.INSTANCE.getComponent(name).getProperty(DevRepository.LOCAL_DEPTH);
		if (dps==null || (dps=dps.trim()).length()==0) {
			dps = Foundation.getProperties().getProperty(DevRepository.LOCAL_DEPTH);
			if (dps!=null && (dps=dps.trim()).length()>0) {
				this.depth = Integer.parseInt(dps);
			}
		}
		
		// get workspaces
		DevRepository.getWorkspaces().forEach(p->{
			File wsf = p.toFile();
			this.wsFolders.add(wsf);
			if (!wsf.exists()) {
				logger.warning("Dev Repository: Workspace folder ("+wsf+") does not exist.");
			}
		});
		if (this.wsFolders.isEmpty()) {
			logger.warning("Dev Repository: No (valid) Workspace folder specified. Dev Repository will not function");
			return;
		}

		// reverse the list so that the given order reflects the priority of the Workspace folders.
		// The scan method just loops over the reversed list and so the priority is ensured.
		Collections.reverse(this.wsFolders);

		logger.info("Dev Repository: Registered "+this);
		this.temp = new File(super.getCacheRoot(),"temp");

		// start it right away
		start();
	}

	@Override
	public void download(FSCRDBComponent component, File folder) {
		String cn = component.getName();
		File s = computeSafePath(this.temp,cn);
		if (!s.exists()) {
			throw new IllegalStateException("Component resources not found in temp space (component: "+cn+", folder: "+s+")");
		}
		String os = System.getProperty("os.name");
		if (os==null || os.contains("indows")) {
			// on windows copy, as rename doesn't seem to work reliably (at least not on win7)
			try {
				FileUtils.copy(s, folder, null);
				FileUtils.delete(s);
			} catch (Exception e) {
				throw new RuntimeException("Failed to move component from temp space (component: "+cn+", folder: "+s+")",e);
			}
		} else  {
			// move
			folder.getParentFile().mkdirs();
			s.renameTo(folder);
		}
	}

	@Override
	public DevRepoDB scan(DevRepoDB oldDB) {
		try {
			DevRepoDB newDB = new DevRepoDB();
			// loop over all Workspace folders
			for (File wsf : this.wsFolders) {
				if (!wsf.exists()) {
					continue;
				}
				// check for modules in workspace folder
				checkModules(wsf,oldDB, newDB, depth);
			}
			return newDB;
		} catch (Exception e) {
			throw new RuntimeException("Dev repository scan failed",e);
		}
	}


	/**
	 * Check for modules in the given folder. We observe depths constraints. Depth
	 */
	private void checkModules(File wsf, DevRepoDB oldDB, DevRepoDB newDB, int depth) throws Exception {
		if (depth>=1 && wsf.isDirectory()) {
			File[] all = wsf.listFiles(NON_HIDDEN_FOLDERS);
			if (all!=null) {
				for (File pf: all) {
					if (new File(pf, "LOCAL").exists()) {
						// go and check components
						_checkArmedComponent(pf, oldDB, newDB);
					} else {
						// go deeper
						checkModules(pf, oldDB, newDB, depth-1);
					}
				}
			}
		}
	}

	/*
	 * The project-folder pf contains a LOCAL file, so it seems to be a z2 module (if pf does not contain a LOCAL file an IllegalStateException is thrown).
	 * Check for simple components (i.e. just a z.properties) or folder-base components and update the dev-repo.
	 */
	private void _checkArmedComponent(File pf, DevRepoDB oldDB, DevRepoDB newDB) throws IOException, Exception, FileNotFoundException {
		FSCRDBComponent c;
		FSCRDBComponent d;

		if (! new File(pf, "LOCAL").exists()) throw new IllegalStateException(pf.getCanonicalPath() + " must contain a LOCAL file!");

		// 1. case, there is a z.properties, directly in this folder.
		// In that case, we assume we are at component level rather than
		// project level. We expect the properties to tell us what project
		// the component belongs to!
		File zp = new File(pf,"z.properties");
		if (zp.exists()) {
			Properties p = _readProps(zp);
			String module = p.getProperty(PROP_PROJECT);
			if (module==null) {
				logger.warning("Found component properties in \""+pf.getName()+"\" but no property \""+PROP_PROJECT+"\" - cannot provide the component.");
			} else {
				// check...
				module=module.trim();
				_checkComponentInFolder(oldDB, newDB, module, pf, zp);
			}
		} else {
			// 2. case: no z.properties in the folder. In that case we assume that
			// there is a whole project. We search child folders and properties files that
			// would define components
			// In addition we add the path to the repository owned paths so that
			// components of that path from other repos would be blocked from visibility
			//
			String module = pf.getName();
			//
			// #2080: Warn about duplicate module
			// 
			if (newDB.hasModule(module)) {
				logger.warning("Found duplicate module definition for "+module+" at "+pf);
			}
			

			File[] cfs = pf.listFiles(NON_HIDDEN_FILES);
			for (File cf:cfs) {
				if (cf.getName().endsWith(PROPERTIES)) {
					String cn = cf.getName();
					cn = module+"/"+cn.substring(0,cn.length()-PROPERTIES.length());
					c = newDB.getComponents().get(cn);
					if (c==null) {
						// we didn't have it before. Properties matter
						long r = cf.lastModified();
						d = (oldDB!=null? oldDB.getComponents().get(cn):null);
						if ((d==null) || (d.getRevision()!=r) || (!d.isHasFile())) {
							// we didn't have it before or in a different revision or not by file
							c = new FSCRDBComponent(cn);
							c.setRevision(r);
							c.setProperties(_readProps(cf));
							logger.fine("Dev Repository: Detected change of component "+cn);
						} else {
							// it was in the old db with the same rev. Assume it is good.
							c=d;
						}
						newDB.putComponent(cn,c);
					}
					c.setHasFile(true);
				} else
				if (cf.isDirectory()) {
					// check for z.properties
					File ppf = new File(cf,Z_PROPERTIES);
					if (ppf.exists()) {
						// ok, its a component folder.
						_checkComponentInFolder(oldDB, newDB, module, cf, ppf);
					}
				}
			}
		}
	}

	// check for a component in a folder and whether it has
	// to be updated etc...
	private void _checkComponentInFolder(
		DevRepoDB odb,
		DevRepoDB ndb,
		String moduleName,
		File componentFolder,
		File zpp
	) throws Exception {
		String cn = moduleName+"/"+componentFolder.getName();
		long r = _computeLM(componentFolder,0);

		FSCRDBComponent c;
		FSCRDBComponent d = (odb!=null? odb.getComponents().get(cn):null);
		if ((d==null) || (d.getRevision()!=r) || (!d.isHasFolder())) {
			// we didn't have it before or in a different revision or not by folder
			c = ndb.getComponents().get(cn);
			if (c==null) {
				c = new FSCRDBComponent(cn);
			} else {
				c.setResolved(false);
			}
			// in case we had a prop file before, we will ignore that for now
			// and override
			c.setRevision(r);
			c.setProperties(_readProps(zpp));
			// copy component resources into temp folder.
			logger.fine("Dev Repository: Copying resources at "+componentFolder+" into temp space");
			File ct = computeSafePath(this.temp,cn);
			if (ct.exists()) {
				FileUtils.delete(ct);
			}
			ct.mkdir();
			FileUtils.copy(componentFolder, ct, NON_HIDDEN_FILES);
			logger.fine("Dev Repository: Detected change of component "+cn);
		} else {
			// it was in the old db with the same rev. Assume it is good.
			c=d;
		}
		ndb.putComponent(cn,c);
		c.setHasFolder(true);
	}

	private Properties _readProps(File zp) throws IOException {
		FileInputStream fin = new FileInputStream(zp);
		try {
			Properties prop = new Properties();
			prop.load(fin);
			// important: Note repo impl component on component properties
			prop.setProperty(COMPONENT_REPO_IMPLEMENTATION, getName());
			return prop;
		} finally {
			fin.close();
		}
	}

	private long _computeLM(File cf, long lm) {
		for (File f:cf.listFiles(NON_HIDDEN_FILES)) {
			lm = Math.max(f.lastModified(), lm);
			if (f.isDirectory()) {
				lm = Math.max(_computeLM(f, lm), lm);
			}
		}
		return lm;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()).append(", workspaces (").append(this.wsFolders.size()).append("): [");
		boolean first = true;
		for (File wsf : this.wsFolders) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			try {
				sb.append(wsf.getCanonicalPath());
			} catch (IOException ioe) {
				sb.append(wsf.getAbsolutePath());
			}
		}
		sb.append("], depth:")
		.append(this.depth);

		return sb.toString();
	}


	//
	// this is a c&p form later core FileUtils. Once the z2-base core is up to date again, remove this
	//

	/**
	 * Compute a good file system location for a possibly path style name relative to some base folder.
	 * That is: Slashes will be kept in the path name but characters in path segments will be replaced if considered problematic.
	 * Furthermore, the first path segment will be augmented by a hash representation of the initial path name to
	 * ensure uniqueness. This method is not transitive, i.e.
	 * <pre>
	 * computeSafePath(base,"a/b")
	 * </pre>
	 * is not equal to
	 * <pre>
	 * computeSafePath(new File(base,"a"),"b")
	 * </pre>
	 *
	 * @param base
	 * @param pathName
	 * @return
	 */
	public static File computeSafePath(File base, String pathName) {
		return new File(base,safePath(pathName));
	}

	/**
	 * Translate a component name into a safe to use relative path in the file system. This is
	 * used to store and organize component data relative to some root folder
	 */
	private static String safePath(String name) {
		StringBuilder sb = new StringBuilder(name.length()<<1);
		sb.append(Integer.toHexString(name.hashCode())).append('-');
		escape(sb,name);
		return sb.toString();
	}

	private static void escape(StringBuilder sb,String in) {
		int l = in.length(),a=0,s=0;
		for (int i=0;i<l;i++) {
			char c = in.charAt(i);
			if (c=='/') {
				sb.append(in.substring(a, i));
				a = i+1;
				// make sure segments are no longer than 200 chars
				int sl = sb.length()-s;
				if (sl>200) {
					sb.setLength(s+200);
				}
				s=i+1;
				sb.append(c);
			} else
			if (!isSafe(c)) {
				sb.append(in.substring(a, i));
				a = i+1;
				sb.append("_");
			}
		}
		if (a<l) {
			sb.append(in.substring(a));
		}
	}

	private static boolean isSafe(char c) {
		return (c>='0' && c<='9') || (c>='a' && c<='z') || (c>='A' && c<='Z') || (c=='/') || (c=='_') || (c=='-');
	}


	private final static Logger logger = Logger.getLogger(DevRepoImpl.class.getName());
}
