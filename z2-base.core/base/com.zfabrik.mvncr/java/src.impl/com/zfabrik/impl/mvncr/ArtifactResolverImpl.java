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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.util.artifact.JavaScopes;

import com.zfabrik.util.fs.FileUtils;
import com.zfabrik.util.runtime.Foundation;

/**
 * Implementation of {@link ArtifactResolver}.
 */
public class ArtifactResolverImpl implements ArtifactResolver {
	
	private final static Logger LOG = Logger.getLogger(ArtifactResolverImpl.class.getName());

	{
		LOG.setLevel(Level.WARNING);
	}
	
	private RepositoryAccess acc;
	private Set<ArtifactName> managed;
	
	public ArtifactResolverImpl(File work, File settingsFile, Collection<ArtifactName> managed) throws Exception {
		this.managed = new HashSet<ArtifactName>(managed);
		this.acc = new RepositoryAccess(new File(work,"local"),settingsFile);
	}
	
	public void clear() {
		this.acc.clear();
	}
	
	public Collection<ArtifactName> resolveDependencies(ArtifactName name, ArtifactResolver.Scope scope, Collection<ArtifactName> exclusions) throws Exception {
    	RepositorySystem sys = acc.getSystem();
		RepositorySystemSession ses = acc.getSystemSession();
		
        Artifact artifact = new DefaultArtifact( name.getGroupId(), name.getArtifactId(), name.getPackaging(), name.getVersion() );

        String javascope;
        switch (scope) {
        case PROVIDED:
        	javascope = JavaScopes.PROVIDED;
        	break;
        case SYSTEM:
        	javascope = JavaScopes.SYSTEM;
        	break;
        case TEST:
        	javascope = JavaScopes.TEST;
        	break;
        case IMPORT:
        case RUNTIME:
        	javascope = JavaScopes.RUNTIME;
        	break;
        case COMPILE:
        default:
        	javascope = JavaScopes.COMPILE;
        	break;
        }

        List<Exclusion> excl = new LinkedList<Exclusion>();
        if (exclusions!=null) {
	        for (ArtifactName a : exclusions) {
	        	//
	        	// for exclusions, we use version in the artifact name as classifier
	        	// and (MOST IMPORTANTLY) default the packaging to JAR
	        	//
	        	
	        	excl.add(new Exclusion(
        			a.getGroupId(), 
        			a.getArtifactId(), 
        			a.getVersion(), 
        			a.getPackaging()==null? ArtifactName.JAR:a.getPackaging()
    			));
	        }
        }
        
        CollectRequest req = new CollectRequest()
        	.setRoot(
    			new Dependency(artifact, javascope)
    			.setExclusions(excl)
    		)
        	.setRepositories(acc.getRemoteRepositories());

        if (this.managed!=null) {
        	//
        	// add managed dependencies (i.e. fixed version vectors)
        	//
        	for (ArtifactName a : this.managed) {
        		req.addManagedDependency(
    				new Dependency(
						new DefaultArtifact(
							a.getGroupId(), 
							a.getArtifactId(),  
							a.getPackaging(),  
							a.getVersion()
						), 
						javascope
					)
				);
        	}
        }
        
        CollectResult res  = sys.collectDependencies(ses, req );
        if (!res.getExceptions().isEmpty()) {
        	throw new RuntimeException("Dependency resolution failed: "+res.getExceptions(), res.getExceptions().get(0));
        }
        
        // check for a local POM. If that is not here now, the whole thing doesn't exist anyway (which aether does not tell us otherwise)
        if (localRepoResolve(new ArtifactName(name.getGroupId(), name.getArtifactId(), name.getVersion(), "pom"),null)==null) {
        	throw new IllegalArgumentException("Failed to resolve "+name);
        }
        
        List<ArtifactName> r = new LinkedList<ArtifactName>();
        Collection<ArtifactName> result = addDeps(r,res.getRoot(), javascope);
		return result;
	}
	
	private Collection<ArtifactName> addDeps(Collection<ArtifactName> r, DependencyNode node, String scope) {
		if (node!=null) {
			Dependency d = node.getDependency();
			if (!d.isOptional() && scope.equalsIgnoreCase(d.getScope())) {
				Artifact a = d.getArtifact();
				r.add(new ArtifactName(a.getGroupId(),a.getArtifactId(),a.getVersion(),a.getExtension()));
				for (DependencyNode dn : node.getChildren()) {
					addDeps(r, dn, scope);
				}
			}
		}
		return r;
	}

	private File localRepoResolve(ArtifactName name, String classifier) throws Exception {
		RepositorySystem sys = acc.getSystem();
		RepositorySystemSession ses = acc.getSystemSession();
        Artifact artifact = new DefaultArtifact( name.getGroupId(), name.getArtifactId(), classifier, name.getPackaging(), name.getVersion() );
        ArtifactRequest req = new ArtifactRequest()
        	.setArtifact(artifact)
        	.setRepositories(acc.getRemoteRepositories());
        try {
        	ArtifactResult res = sys.resolveArtifact(ses, req);
        	// check exceptions
	        if ((res.getArtifact()==null || res.getArtifact().getFile()==null) && classifier==null) {
	        	if (!res.getExceptions().isEmpty()) {
	        		throw new RuntimeException("Dependency retrieval of "+artifact+" failed: "+res.getExceptions(), res.getExceptions().get(0));
	        	}
        		throw new RuntimeException("Dependency retrieval of "+artifact+" failed (for unknown reasons)");
	        }
	        if (res.getArtifact()!=null) {
	        	return res.getArtifact().getFile();
	        } 
        } catch (ArtifactResolutionException are) {
        	if (classifier==null) {
	        	throw are;
        	}
        }
        return null;
	}
	
	public Collection<File> download(ArtifactName name, File out) throws Exception {
		List<File> files = new LinkedList<File>();
		File f = localRepoResolve(name,null);
		if (f==null) {
			throw new IllegalStateException("Failed to retrieve "+name);
		} else {
	        File o = new File(out,f.getName());
	        FileUtils.copy(f, o, null);
	        files.add(o);
	        if (Foundation.isDevelopmentMode()) {
	        	// also retrieve sources
		        File sf = localRepoResolve(name, "sources");
		        if (sf!=null) {
			        File so = new File(out,sf.getName());
			        FileUtils.copy(sf, so, null);
		        	files.add(so);
		        } else {
		        	LOG.info("Source artifact for "+name+" not found.");
		        }
	        }
		}
		return files;
	}
	
}
