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
import java.util.Collection;

import com.zfabrik.impl.mvncr.ArtifactName;
import com.zfabrik.impl.mvncr.ArtifactResolverImpl;
import com.zfabrik.util.fs.FileUtils;

public class DownloadArtifactFromMavenRepo {


	public static void main(String[] args) throws Exception {
		File work = new File("work");
//		FileUtils.delete(work);
		resolve(ArtifactName.parse("log4j:log4j:jar:1.2.17"), work);
		resolve(ArtifactName.parse("org.vaadin.addons.lazyquerycontainer:vaadin-lazyquerycontainer:2.1.15"),work);
		resolve(ArtifactName.parse("org.springframework:spring-aop:4.0.2.RELEASE"),work);

	}

	private static void resolve(ArtifactName n, File work) throws Exception {
		ArtifactResolverImpl ar = new ArtifactResolverImpl(work, new File("conf/settings.xml"),null);
		File out = new File("out");
		FileUtils.delete(out);
		out.mkdirs();
		Collection<File> r = ar.download(n , out);
		for (File an : r) {
			System.out.println(an);
		}
	}
}
