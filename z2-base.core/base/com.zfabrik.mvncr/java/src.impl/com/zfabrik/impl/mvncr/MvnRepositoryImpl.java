/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.mvncr;

import java.io.File;
import java.security.MessageDigest;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.provider.IComponentsRepository;
import com.zfabrik.components.provider.util.AbstractComponentRepository;
import com.zfabrik.components.provider.util.FSCRDBComponent;
import com.zfabrik.components.provider.util.OfflineModeException;
import com.zfabrik.resources.IResourceHandle;
import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.html.Escaper;
import com.zfabrik.util.runtime.Foundation;

/**
 * Actual {@link IComponentsRepository} implementation based on {@link ArtifactResolverImpl}.
 * See {@link MvnRepositoryResource} for documentation.
 */
public class MvnRepositoryImpl extends AbstractComponentRepository<MvnRepositoryDB> {
	private final static Logger LOG = Logger.getLogger(MvnRepositoryImpl.class.getName());


    /**
	 * Component property. Symbolic name of the repository, referenced by fragments using the same property.
	 */
	public static final String MVN_REPOSITORY	    = "mvncr.repository";

	/**
	 * Component property. Fixed artifact versions, if encountered during recursive root resolution.
	 * This corresponds to a &lt;dependencyManagement&gt; section in a Maven POM file
	 */
	public static final String MVN_MANAGED	    = "mvncr.managed";
    /**
	 * Component property. A comma separated list of artifacts that will be skipped during resolution of any root
	 */
	public static final String MVN_EXCLUDED		= "mvncr.excluded";
    /**
	 * Component property. Specifies the location of the settings XML file relative to the components resources.
	 * Defaults to <code>settings.xml</code>
	 */
	public static final String MVN_SETTINGS 	= "mvncr.settings";
    /**
	 * Component property. A comman-separated list of root artifacts. See {@link MvnRepositoryResource} for more details.
	 */
	public static final String MVN_ROOTS 		= "mvncr.roots";
    /**
	 * Component property. The repository priority in repository chaining as defined in {@link IComponentsRepository}.
	 * Defaults to 500.
	 */
	public static final String MVN_PRIO 		= "mvncr.priority";

	private static final String MVN_ARTIFACTNAME = MvnRepositoryImpl.class.getName()+".artifactName";

	/**
	 * Dependency fine-tuning. See {@link MvnRepositoryResource} for more details.
	 */
	public static final String QUERY_EXCLUDED 	= "excluded";
	/**
	 * Dependency fine-tuning. See {@link MvnRepositoryResource} for more details.
	 */
	public static final String QUERY_SCOPE 	= "scope";
	/**
	 * Dependency fine-tuning. See {@link MvnRepositoryResource} for more details.
	 */
	public static final String QUERY_VERSIONED = "versioned";

	// helper struct
	private static class ResolutionConfig {
		ArtifactResolver.Scope scope = ArtifactResolver.Scope.COMPILE;
		boolean versioned = false;
		Set<ArtifactName> excluded;

		public String toString() {
			return "{scope: " + scope + ", versioned: " + versioned + ", exluded: "+excluded+"}";
		}
	}
	// END

	// helper struct
	private static class ComponentTarget {
		private ArtifactName name;
		private ResolutionConfig rc;

		public ComponentTarget(ArtifactName name, ResolutionConfig rc) {
			super();
			this.name = name;
			this.rc = rc;
		}

		@Override
		public String toString() {
			return "{name: " + name + ", rc: " + rc + "}";
		}
	}
	// END

	private Map<ArtifactName,ResolutionConfig> roots;
	private Set<ArtifactName> managed,excluded;
	private String mvncr_repository;
	private ArtifactResolver ar;
	private byte[] settingsHash;

	public MvnRepositoryImpl(String name, Properties props) throws Exception {
		super(name, MvnRepositoryDB.class);

		// parse prio
		int prio;
		String prios = props.getProperty(MVN_PRIO);
		if (prios == null || (prios = prios.trim()).length() == 0) {
			prio = 500;
		} else {
			try {
				prio = Integer.parseInt(prios);
			} catch (NumberFormatException nfe) {
				throw new IllegalStateException("Invalid priority (" + MVN_PRIO + ") specification (" + prios + "): " + this.getName(), nfe);
			}
		}
		configure(prio);
		
		this.mvncr_repository=props.getProperty(MVN_REPOSITORY, name).trim();
	

		/*
		 * Parse configuration and merge across fragments.
		 */
		MvnConfigCollector c = new MvnConfigCollector(IComponentsLookup.INSTANCE.lookup(name, IResourceHandle.class));
		c.add(name);

		/*
		 * Handle Roots
		 *
		 * Root syntax is: <artifact>{?versioned=<boolean>&scope=<scope>}
		 */
		Map<ArtifactName,ResolutionConfig> ns = new HashMap<ArtifactName,ResolutionConfig>();
		for (String an : c.getRoots()) {
			if (an!=null) {
				ResolutionConfig rc = new ResolutionConfig();

				int p = an.lastIndexOf('?');
				if (p>0) {
					Properties q = query(an.substring(p));
					// params
					rc.versioned = Boolean.parseBoolean(q.getProperty(QUERY_VERSIONED,"false"));
					rc.scope=ArtifactResolver.Scope.valueOf(q.getProperty(QUERY_SCOPE,ArtifactResolver.Scope.COMPILE.name()).toUpperCase());
					rc.excluded=parseArtifacts(q.getProperty(QUERY_EXCLUDED,""));
					an = an.substring(0,p);
				}
				ns.put(ArtifactName.parse(an),rc);
			}
		}
		this.roots = ns;

		/*
		 * Handle managed dependencies, i.e. fixed version vectors
		 */
		this.managed = parseArtifacts(c.getManaged());
		/*
		 * Parse globally excluded dependencies
		 */
		this.excluded = parseArtifacts(c.getExcluded());

		File root = IComponentsManager.INSTANCE.retrieve(name);
		File settings = new File(root,props.getProperty(MVN_SETTINGS, ArtifactResolver.DEFAULT_SETTINGS_FILE).trim());

		this.settingsHash = computeFileHash(settings);
		File work = FileUtils.computeSafePath(new File(new File(Foundation.getProperties().getProperty(Foundation.HOME)),"work/mvn"),name.replace('/', '_'));
		this.ar = new ArtifactResolverImpl(work, settings, managed);

		if (!Foundation.isWorker()) {
			LOG.info("Using " + this.toString());
		}
	}

	private Set<ArtifactName> parseArtifacts(Set<String> mr) {
		Set<ArtifactName> res = new HashSet<ArtifactName>();
		for (String an : mr) {
			if (an!=null) {
				res.add(ArtifactName.parse(an));
			}
		}
		return res;
	}

	private Set<ArtifactName> parseArtifacts(String mr) {
		Set<ArtifactName> res = new HashSet<ArtifactName>();
		if (mr!=null) {
			StringTokenizer tk = new StringTokenizer(mr,",");
			while (tk.hasMoreTokens()) {
				String an = StringUtils.trimToNull(tk.nextToken());
				if (an!=null) {
					res.add(ArtifactName.parse(an));
				}
			}
		}
		return res;
	}

	private Properties query(String q) {
		StringTokenizer tk = new StringTokenizer(q, "&");
		Properties p = new Properties();
		while (tk.hasMoreTokens()) {
			String nv = tk.nextToken().trim();
			int s = nv.lastIndexOf('=');
			if (s>0) {
				p.put(
					Escaper.urlDecode(nv.substring(0,s)).trim(),
					Escaper.urlDecode(nv.substring(s+1)).trim()
				);
			}
		}
		return p;
	}

	@Override
	public void download(FSCRDBComponent component, File folder) {
		try {
			String san = component.getProperty(MVN_ARTIFACTNAME);
			if (san==null) {
				throw new IllegalStateException("Component "+component.getName()+" is lacking property "+MVN_ARTIFACTNAME);
			}
			LOG.info("Providing "+san);
			ArtifactName an = ArtifactName.parse(san);
			File binApi = new File(folder,"bin.api/lib");
			binApi.mkdirs();
			this.ar.download(an, binApi);
		} catch (Exception e) {
			throw new RuntimeException("Failed to download "+component.getName(),e);
		}
	}

	@Override
	public MvnRepositoryDB scan(MvnRepositoryDB current) {
		long start = System.currentTimeMillis();
		try {
			checkOfflineMode();
			if (current==null || LOG.isLoggable(Level.FINE)) {
				LOG.info("Scanning for "+this.roots.size()+" roots. This may take a moment...");
			}
//			boolean clear = current==null;
//			if (!clear && !Arrays.equals(current.getSettingsHash(),this.settingsHash)) {
//				LOG.info("Found updated repository settings. Clearing cache");
//				clear = true;
//			}
//			if (clear) {
//				this.ar.clear();
//			}

			//
			// in a first pass, we resolve all dependencies to component names
			// and make sure to choose the highest version artifacts available
			//
			// component name -> effective artifact (for non-versioned components this may require version-conflict resolution)
			//
			Map<String,ComponentTarget> c2a = new HashMap<String, ComponentTarget>();
			for (Map.Entry<ArtifactName,ResolutionConfig> e : this.roots.entrySet()) {
				if (current==null || LOG.isLoggable(Level.FINE)) {
					LOG.info("Resolving root "+e.getKey()+" "+e.getValue());
				}
				resolve(new HashSet<ArtifactName>(),c2a,e.getKey(),e.getValue());
			}


			MvnRepositoryDB db = new MvnRepositoryDB();
			db.setSettingsHash(settingsHash);

			//
			// in a second pass, we create components
			//
			for (Map.Entry<String,ComponentTarget> e : c2a.entrySet()) {
				buildComponent(db,e.getKey(),e.getValue());
			}

			if (LOG.isLoggable(Level.FINER)) {
				LOG.fine("Resolved the following component:");
				for (String cn : db.getComponents().keySet()) {
					LOG.finer(cn);
				}
			}
			Set<String> all = new HashSet<String>(db.getComponents().keySet());
			if (current!=null) {
				all.removeAll(current.getComponents().keySet());
			}
			long duration = System.currentTimeMillis()-start;
			LOG.info("Scan completed and resolved "+all.size()+" new components after "+duration+"ms");
			return db;
		} catch (Exception e) {
			if (relaxing(e) && current!=null) {
				return current;
			}
			throw new RuntimeException("scan failed",e);
		}
	}

	private boolean relaxing(Exception e) {
		if (e instanceof OfflineModeException) {
			LOG.warning("System is running in offline mode. Will continue without attempting remote access: " + this.getName());
			return true;
		}
		if (this.isRelaxedMode()) {
			LOG.warning("MVN-CR scan failed ("+e.getMessage()+"). Ignoring as we are in relaxed mode: " + this.getName());
			return true;
		}
		return false;
	}


	// resolve with dependencies and version maximizing
	private void resolve(Set<ArtifactName> handled, Map<String, ComponentTarget> c2a, ArtifactName an, ResolutionConfig rc) throws Exception {
		if (handled.contains(an)) {
			return;
		}
		handled.add(an);

		Set<ArtifactName> ex = new HashSet<ArtifactName>(excluded);
		if (rc.excluded!=null) {
			ex.addAll(rc.excluded);
		}
    	LOG.fine("Resolution for "+an+" is using exclusions "+ex);
		Collection<ArtifactName> ns = this.ar.resolveDependencies(an,rc.scope,ex);
		for (ArtifactName n : ns) {
        	LOG.fine("Resolution for "+an+" resolved "+n);
			String cn = n.toComponentName(rc.versioned);
			ComponentTarget ct = c2a.get(cn);
			if (ct!=null) {
				// check, if we replace
				int c = ct.name.versionCompare(n);
				if (c<0) {
					LOG.info("Choosing "+n+" rather than "+ct.name);
					ct.name = n;
					ct.rc = rc;
				} else
				if (c>0) {
					LOG.info("Choosing "+ct.name+" rather than "+n);
				}
			} else {
				c2a.put(cn, new ComponentTarget(n,rc));
			}
		}
		// dive in
		for (ArtifactName n : ns) {
			resolve(handled, c2a,n,rc);
		}
	}

	// build components
	private void buildComponent(MvnRepositoryDB db, String cn, ComponentTarget target) throws Exception {
		if (db.getComponents().containsKey(cn)) {
			return;
		}
		FSCRDBComponent c = new FSCRDBComponent(cn);
		// fill props
		Properties p = new Properties();
		p.put(IComponentDescriptor.COMPONENT_TYPE,IJavaComponent.TYPE);
		// compute pub refs
		StringBuilder b = new StringBuilder();

		Set<ArtifactName> ex = new HashSet<ArtifactName>(excluded);
		if (target.rc.excluded!=null) {
			ex.addAll(target.rc.excluded);
		}
		Collection<ArtifactName> ns = this.ar.resolveDependencies(target.name,target.rc.scope,ex);
		for (ArtifactName n : ns) {
			if (!n.equals(target.name)) {
				if (b.length()>0) {
					b.append(",");
				}
				b.append(n.toComponentName(target.rc.versioned));
			}
		}
		p.put(IJavaComponent.PUBREFS, b.toString());
		p.put(MVN_ARTIFACTNAME, target.name.toString());

		c.setHasFile(false);
		c.setHasFolder(true);
		c.setRevision(((long) Integer.MAX_VALUE) + target.name.getVersion().hashCode());
		p.put(IComponentDescriptor.REVISION_INFO, target.name.getVersion());
		c.setProperties(p);
		db.putComponent(cn, c);
	}

	private byte[] computeFileHash(File file) throws Exception {
		MessageDigest md = MessageDigest.getInstance("MD5");
		return md.digest(org.apache.commons.io.FileUtils.readFileToByteArray(file));
	}

	@Override
	public String toString() {
		return "MVN-CR: mvncr.repository:"+this.mvncr_repository+","+super.toString();
	}

}