/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.transfer;

import java.util.HashMap;
import java.util.Map;

import org.junit.platform.engine.reporting.ReportEntry;

/**
 * Serialized representation of a {@link ReportEntry}
 */
public class Z2JupiterReportEntryDto {
	private Map<String,String> keyValuePairs;

	public Z2JupiterReportEntryDto() {}
	
	public Z2JupiterReportEntryDto(ReportEntry entry) {
		this.keyValuePairs=new HashMap<>(entry.getKeyValuePairs());
	}

	public Map<String, String> getKeyValuePairs() {
		return keyValuePairs;
	}

	public void setKeyValuePairs(Map<String, String> keyValuePairs) {
		this.keyValuePairs = keyValuePairs;
	}

	public ReportEntry toReportEntry() {
		return ReportEntry.from(this.keyValuePairs);
	}
}
