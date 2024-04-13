/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.mvncr;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.filter.DependencyFilterUtils;

import com.zfabrik.impl.mvncr.ArtifactName;
import com.zfabrik.impl.mvncr.RepositoryAccess;

public class ResolveDependencies {

	public static void main(String[] args) throws Exception {
		
		File target = new File("mvn/local");
		File settings = new File("../../z2-base.base/environment.base/mavenDefault/settings.xml");
		if (!settings.exists()) {
			throw new IllegalStateException();
		}
		target.mkdirs();
		RepositoryAccess acc = new RepositoryAccess(target,settings);
		RepositorySystem sys = acc.getSystem();
		RepositorySystemSession ses = acc.getSystemSession();
		
        Artifact artifact = new DefaultArtifact( "org.springframework:spring-context:5.0.6.RELEASE" );

		
//        Artifact artifact = new DefaultArtifact( "com.jamonapi:jamon:2.4" );

        DependencyFilter classpathFlter = DependencyFilterUtils.classpathFilter( JavaScopes.COMPILE );

        CollectRequest collectRequest = new CollectRequest();
        collectRequest.setRoot( 
        		new Dependency( artifact, JavaScopes.COMPILE )
        		.setExclusions(Arrays.asList(new Exclusion("org.springframework", "spring-jcl", "", "jar")))
        );
        
        collectRequest.setRepositories( acc.getRemoteRepositories() );

        DependencyRequest dependencyRequest = new DependencyRequest( collectRequest, classpathFlter );

        List<ArtifactResult> artifactResults = sys.resolveDependencies(ses, dependencyRequest ).getArtifactResults();

        for (ArtifactResult artifactResult : artifactResults )
        {
            System.out.println( artifactResult.getArtifact() + " resolved to " + artifactResult.getArtifact().getFile() );
        }
		
	}

}
