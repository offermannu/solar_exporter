/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.mvncr.util;

import java.util.logging.Logger;

import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;

public class LoggingRepositoryListener implements RepositoryListener {
	private final static Logger LOG = Logger.getLogger(LoggingRepositoryListener.class.getName());
	
	public void artifactDeployed(RepositoryEvent event) {
		LOG.finer("Deployed " + event.getArtifact() + " to " + event.getRepository());
	}

	public void artifactDeploying(RepositoryEvent event) {
		LOG.fine("Deploying " + event.getArtifact() + " to " + event.getRepository());
	}

	public void artifactDescriptorInvalid(RepositoryEvent event) {
		LOG.warning("Invalid artifact descriptor for " + event.getArtifact() + ": " + event.getException().getMessage());
	}

	public void artifactDescriptorMissing(RepositoryEvent event) {
		LOG.warning("Missing artifact descriptor for " + event.getArtifact());
	}

	public void artifactInstalled(RepositoryEvent event) {
		LOG.fine("Installed " + event.getArtifact() + " to " + event.getFile());
	}

	public void artifactInstalling(RepositoryEvent event) {
		LOG.fine("Installing " + event.getArtifact() + " to " + event.getFile());
	}

	public void artifactResolved(RepositoryEvent event) {
		LOG.finer("Resolved artifact " + event.getArtifact() + " from " + event.getRepository());
	}

	public void artifactDownloading(RepositoryEvent event) {
		LOG.fine("Downloading artifact " + event.getArtifact() + " from " + event.getRepository());
	}

	public void artifactDownloaded(RepositoryEvent event) {
		LOG.fine("Downloaded artifact " + event.getArtifact() + " from " + event.getRepository());
	}

	public void artifactResolving(RepositoryEvent event) {
		LOG.finer("Resolving artifact " + event.getArtifact());
	}

	public void metadataDeployed(RepositoryEvent event) {
		LOG.finer("Deployed " + event.getMetadata() + " to " + event.getRepository());
	}

	public void metadataDeploying(RepositoryEvent event) {
		LOG.finer("Deploying " + event.getMetadata() + " to " + event.getRepository());
	}

	public void metadataInstalled(RepositoryEvent event) {
		LOG.fine("Installed " + event.getMetadata() + " to " + event.getFile());
	}

	public void metadataInstalling(RepositoryEvent event) {
		LOG.fine("Installing " + event.getMetadata() + " to " + event.getFile());
	}

	public void metadataInvalid(RepositoryEvent event) {
		LOG.warning("Invalid metadata " + event.getMetadata());
	}

	public void metadataResolved(RepositoryEvent event) {
		LOG.finer("Resolved metadata " + event.getMetadata() + " from " + event.getRepository());
	}

	public void metadataResolving(RepositoryEvent event) {
		LOG.finer("Resolving metadata " + event.getMetadata() + " from " + event.getRepository());
	}
	
	@Override
	public void metadataDownloaded(RepositoryEvent event) {
		LOG.finer("Downloaded metadata " + event.getMetadata() + " from " + event.getRepository());
	}
	@Override
	public void metadataDownloading(RepositoryEvent event) {
		LOG.finer("Downloading metadata " + event.getMetadata() + " from " + event.getRepository());
	}
}
