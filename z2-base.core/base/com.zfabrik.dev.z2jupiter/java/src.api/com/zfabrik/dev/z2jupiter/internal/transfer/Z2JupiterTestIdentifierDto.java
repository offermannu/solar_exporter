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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.platform.engine.TestDescriptor.Type;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.TestIdentifier;

/**
 * Serialization for TestIdentifier
 */
public class Z2JupiterTestIdentifierDto {
	private String displayName;
	private String legacyReportingName;
	private String parentId;
	private Z2JupiterTestSourceDto source;
	private String uniqueId;
	private Type type;
	private Set<TestTag> tags;
	private List<Z2JupiterTestIdentifierDto> children;
	
	public Z2JupiterTestIdentifierDto() {
	}
	
	public Z2JupiterTestIdentifierDto(TestIdentifier testIdentifier) {
		this.type = testIdentifier.getType();
		this.tags = testIdentifier.getTags();
		this.uniqueId = testIdentifier.getUniqueId();
		this.displayName = testIdentifier.getDisplayName();
		this.legacyReportingName = testIdentifier.getLegacyReportingName();
		this.parentId = testIdentifier.getParentId().orElse(null);
		this.source = testIdentifier.getSource().map(Z2JupiterTestSourceDto::new).orElse(null);
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getLegacyReportingName() {
		return legacyReportingName;
	}

	public void setLegacyReportingName(String legacyReportingName) {
		this.legacyReportingName = legacyReportingName;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public Z2JupiterTestSourceDto getSource() {
		return source;
	}

	public void setSource(Z2JupiterTestSourceDto source) {
		this.source = source;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Set<TestTag> getTags() {
		return tags;
	}

	public void setTags(Set<TestTag> tags) {
		this.tags = tags;
	}

	public List<Z2JupiterTestIdentifierDto> getChildren() {
		if (this.children == null) {
			this.children = new ArrayList<>();
		}
		return children;
	}
	
	public void setChildren(List<Z2JupiterTestIdentifierDto> children) {
		this.children = children;
	}
	
}
