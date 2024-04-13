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
import java.util.Collection;

import com.zfabrik.impl.mvncr.ArtifactName;
import com.zfabrik.impl.mvncr.ArtifactResolver;
import com.zfabrik.impl.mvncr.ArtifactResolver.Scope;
import com.zfabrik.impl.mvncr.ArtifactResolverImpl;

public class ResolveDependenciesFromMavenRepo {

	public static void main(String[] args) throws Exception {
		File work = new File("work");
//		FileUtils.delete(work);
		
//		resolve(ArtifactName.parse("org.vaadin.addons.lazyquerycontainer:vaadin-lazyquerycontainer:2.1.15"),work);
//		resolve(ArtifactName.parse("org.springframework:spring-aop:4.0.2.RELEASE"),work);
//		resolve(ArtifactName.parse("com.jamonapi:jamon:2.4"),work);
		resolve(ArtifactName.parse("org.springframework:spring-context:4.0.2.RELEASE"), work);
//		resolve(ArtifactName.parse("org.springframework:spring-core:4.0.2.RELEASE"), work);
//		resolve(ArtifactName.parse("org.springframework.security:spring-security-core:3.1.0.RELEASE"), work);
	}

	private static void resolve(ArtifactName n, File work) throws Exception {
		ArtifactResolverImpl ar = new ArtifactResolverImpl(
			work, 
			new File("conf/settings.xml"),
			Arrays.asList(new ArtifactName[]{ArtifactName.parse("commons-logging:commons-logging:jar:1.1.2")})
		);
		Collection<ArtifactName> r = ar.resolveDependencies(
			n, 
			ArtifactResolver.Scope.COMPILE,
			Arrays.asList(new ArtifactName[]{ArtifactName.parse("org.springframework:spring-expression:jar:4.0.2.RELEASE")})
		);
		for (ArtifactName an : r) {
			System.out.println(an);
		}
	}
}
