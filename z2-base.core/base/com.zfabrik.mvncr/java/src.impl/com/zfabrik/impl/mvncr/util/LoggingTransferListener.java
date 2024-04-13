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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.logging.Logger;

import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;

public class LoggingTransferListener implements TransferListener {
	private final static Logger LOG = Logger.getLogger(LoggingTransferListener.class.getName());
	
	@Override
	public void transferInitiated(TransferEvent event) {
		String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";
		LOG.fine(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
	}

	@Override
	public void transferProgressed(TransferEvent event) {}

	@Override
	public void transferStarted(TransferEvent event) throws TransferCancelledException {}

	@Override
	public void transferSucceeded(TransferEvent event) {
		TransferResource resource = event.getResource();
		long contentLength = event.getTransferredBytes();
		if (contentLength >= 0) {
			String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
			String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

			String throughput = "";
			long duration = System.currentTimeMillis() - resource.getTransferStartTime();
			if (duration > 0) {
				long bytes = contentLength - resource.getResumeOffset();
				DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
				double kbPerSec = (bytes / 1024.0) / (duration / 1000.0);
				throughput = " at " + format.format(kbPerSec) + " KB/sec";
			}

			LOG.fine(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput + ")");
		}
	}

	@Override
	public void transferFailed(TransferEvent event) {
		if (!(event.getException() instanceof MetadataNotFoundException)) {
			LOG.fine("Failed to transfer "+event.getResource().getResourceName()+" ("+event.getException()+")");
		}
	}

	public void transferCorrupted(TransferEvent event) {
		throw new RuntimeException("Failed to download "+event.getResource().getResourceName(),event.getException());
	}

	protected long toKB(long bytes) {
		return (bytes + 1023) / 1024;
	}



}
