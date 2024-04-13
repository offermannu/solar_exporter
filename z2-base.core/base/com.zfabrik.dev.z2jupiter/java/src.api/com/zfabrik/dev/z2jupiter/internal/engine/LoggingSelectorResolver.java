/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.dev.z2jupiter.internal.engine;

import java.util.logging.Logger;

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
import org.junit.platform.engine.support.discovery.SelectorResolver;

public abstract class LoggingSelectorResolver implements SelectorResolver {
	private final static Logger LOG = Logger.getLogger(LoggingSelectorResolver.class.getName());

	/**
	 * Discovery for a class 
	 */
	@Override
	public Resolution resolve(ClassSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(ClassSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}
	
	/**
	 * Discovery for a method
	 */
	@Override
	public Resolution resolve(MethodSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(MethodSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(ClasspathResourceSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(ClasspathResourceSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}
	
	@Override
	public Resolution resolve(ClasspathRootSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(ClasspathRootSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(NestedClassSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(NestedClassSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(DirectorySelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(DirectorySelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(FileSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(FileSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(NestedMethodSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(NestedMethodSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(ModuleSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(ModuleSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(PackageSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(PackageSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(UniqueIdSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(UniqueIdSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(UriSelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(UriSelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}

	@Override
	public Resolution resolve(DiscoverySelector selector, Context context) {
		LOG.info("Z2UnitSelectorResolver.resolve(DiscoverySelector selector, Context context)");
		return SelectorResolver.super.resolve(selector, context);
	}
	

}
