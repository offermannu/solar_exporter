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

/**
 * Artifact resolution interface. Used by the maven component repository implementations. 
 *   
 */
public interface ArtifactResolver {

	/**
	 * Resolution scope closely related to Maven dependency scopes
	 * @author hb
	 *
	 */
	public static enum Scope {
		COMPILE, RUNTIME, PROVIDED, TEST, SYSTEM, IMPORT
	}

	String DEFAULT_SETTINGS_FILE = "settings.xml";

	/**
	 * Drop the local repository cache
	 */
	public abstract void clear();

	/**
	 * Resolve dependencies of an artifact taking into account scope and exclusions. 
	 * 
	 * @param name Artifact to resolve for. This will also be listed with the result
	 * @param scope Scope to resolve for (implies recursiveness)
	 * @param exclusions Exclusions to be omitted from the dependency graph
	 * @return A collection of artifact names making up the dependencies of the artifact to resolve for.
	 * @throws Exception
	 */
	public abstract Collection<ArtifactName> resolveDependencies(ArtifactName name, ArtifactResolver.Scope scope, Collection<ArtifactName> exclusions) throws Exception;

	/**
	 * Download or provide from cache all artifact file resources in the specified output folder. The result may encompass more than 
	 * a single artifact. If running in development mode for example, source attachments will be provided as well. 
	 * 
	 * @param name Artifact to retrieve
	 * @param out Output folder.
	 * @return a collection of all files as provided to the output folder 
	 * @throws Exception
	 */
	public abstract Collection<File> download(ArtifactName name, File out) throws Exception;

}