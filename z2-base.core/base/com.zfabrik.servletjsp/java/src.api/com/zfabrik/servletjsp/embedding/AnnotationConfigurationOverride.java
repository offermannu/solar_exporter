/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.servletjsp.embedding;

import javax.servlet.ServletContainerInitializer;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * See #2000. This is an override for Jetty's Annotation Config to correctly
 * decide on container level jars, vs. application level jars.
 */
public class AnnotationConfigurationOverride extends AnnotationConfiguration {
	
	@Override
	public boolean isFromContainerClassPath(WebAppContext context, ServletContainerInitializer sci) {
       if (sci == null)
            return false;
       // simply rely on container==everything in jetty module
       return sci.getClass().getClassLoader()==this.getClass().getClassLoader();
	}

}
