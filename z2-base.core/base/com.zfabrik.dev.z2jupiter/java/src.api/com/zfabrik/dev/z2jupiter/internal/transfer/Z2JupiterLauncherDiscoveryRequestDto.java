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
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.DirectorySelector;
import org.junit.platform.engine.discovery.FileSelector;
import org.junit.platform.engine.discovery.MethodSelector;
import org.junit.platform.engine.discovery.ModuleSelector;
import org.junit.platform.engine.discovery.NestedClassSelector;
import org.junit.platform.engine.discovery.NestedMethodSelector;
import org.junit.platform.engine.discovery.PackageSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.discovery.UriSelector;
import org.junit.platform.launcher.LauncherDiscoveryRequest;

import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;

/**
 * Serializable launcher discovery request
 */
public class Z2JupiterLauncherDiscoveryRequestDto {
	
	/** 
	 * Supported selectors to be put in extensible config or in {@link Z2JupiterTestable}
	 */
	private final static List<Class<? extends DiscoverySelector>> SELECTORS = new ArrayList<>();
	static {
		SELECTORS.add(ClasspathResourceSelector.class);
		SELECTORS.add(ClasspathRootSelector.class);
		SELECTORS.add(ClassSelector.class);
		SELECTORS.add(DirectorySelector.class);
		SELECTORS.add(FileSelector.class);
		SELECTORS.add(MethodSelector.class);
		SELECTORS.add(ModuleSelector.class);
		SELECTORS.add(NestedClassSelector.class);
		SELECTORS.add(NestedMethodSelector.class);
		SELECTORS.add(PackageSelector.class);
		SELECTORS.add(UniqueIdSelector.class);
		SELECTORS.add(UriSelector.class);
	}
	
	private List<Z2JupiterSelectorDto> selectors = new LinkedList<>();
	
	public Z2JupiterLauncherDiscoveryRequestDto() {}
	
	public Z2JupiterLauncherDiscoveryRequestDto(LauncherDiscoveryRequest request) {
		/*
		 * Note: We only care about selectors. Filters are applied in the backend
		 * during default discovery. I.e. we assume that the client view and the server
		 * view are identical as far as engines are concerned
		 */
		this.selectors = SELECTORS.stream()
			.map(t->request.getSelectorsByType(t))
			.flatMap(l->l.stream())
			.map(Z2JupiterSelectorDto::new)
			.collect(Collectors.toList());
	}

	public List<Z2JupiterSelectorDto> getSelectors() {
		return selectors;
	}
	
	public void setSelectors(List<Z2JupiterSelectorDto> selectors) {
		this.selectors = selectors;
	}
}
