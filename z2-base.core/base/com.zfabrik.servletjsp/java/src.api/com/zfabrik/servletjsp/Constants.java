/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.servletjsp;

import com.zfabrik.components.java.IJavaComponent;

/**
 * Web applications are components of type <code>com.zfabrik.ee.webapp</code>.
 * <p> 
 * Technically the Web app handling in z2 is just a thin wrapper 
 * around Jetty WebAppContexts.
 * <p>
 * A Web app can have the following web app specific properties:
 * 
 * <dl>
 * 
 * <dt>webapp.server</dt>
 * <dd>The server component this web app is supposed to run on. This property
 * is required. The usual value is <code>environment/webServer</code>.</dd>
 * 
 * <dt>webapp.path</dt>
 * <dd>The context path of the Web app. This property is required. It MUST start with a "/".</dd>
 * 
 * <dt>webapp.requiredPaths</dt>
 * <dd>A Web app may specify a comma separated list of other Web apps that it requires to
 * run. This is very useful if Web app re-use each others resources. The required Web apps
 * are specified by there context path, e.g. <code>webapp.requiredPaths=/a, /second, /somemore</code>.</dd>
 * 
 * </dl>
 * Typically a Web app will define some system state participation, e.g. 
 * <pre>
 * com.zfabrik.systemState.participation=environment/webWorkerUp
 * </pre>
 * that defines under what circumstances the Web app will actually be initialized. 
 * <p> 
 * Web apps components have a resource folder. That folder must contain a sub folder <code>WebContent</code> 
 * that holds the standard Java Web application structure of an expanded WAR file. In particular the 
 * <code>WebContent</code> folder typically has a <code>WEB-INF</code> sub folder that contains
 * the deployment descriptor <code>web.xml</code>.
 * <p>
 * A typical Web app component looks like this:
 * <pre>
 * z.properties
 * WebContent/
 * 	WEB-INF/
 * 		web.xml
 * 	index.html
 * </pre> 
 * <p>
 * All definitions in the <code>WebContent</code> folder will be interpreted by Jetty. Jetty
 * specific deployment descriptors (and anything else Jetty specific) can be supplied as 
 * described in the Jetty documentation.
 * <p>
 * Following the standard, Web app components may provide libraries and Java classes in 
 * <code>WebContent/WEB-INF/lib</code> and <code>WebContent/WEB-INF/classes</code> resp.
 * <p>
 * In z2 however the Web app component does not hold the Web app's Java source code. Instead
 * the component's default Java component serves as the main venue to provide
 * the Web app's implementation. See also {@link IJavaComponent}. 
 * 
 * @author hb
 *
 */
public final class Constants {
	/**
	 * Component type of web apps.
	 */
	public final static String WEBAPP_TYPE = "com.zfabrik.ee.webapp";
	
	/**
	 *  set this in the request as attribute to mark a request as a page impression
	 */
	public final static String ATTRIBUTE_IS_PAGE_IMPRESSION = "com.zfabrik.servletjsp.pageImpression";

	/**
	 * A Web app may specify a comma separated list of other Web apps that it requires to
     * run. This is very useful if Web app re-use each others resources. The required Web apps
     * are specified by there context path, e.g. <code>webapp.requiredPaths=/a, /second, /somemore</code>.
	 * 
	 */
	public final static String REQUIRED_PATHS = "webapp.requiredPaths";

	/** 
	 * The jars that qualify for meta-data parsing can be limited in Jetty. In z2 the default is defined by the
	 * regular expression "(?!.+-sources\\.).*\\.jar$" (as Java string) which is all Jars except the source code jars (by convention). 
	 * (See also <a href="http://www.eclipse.org/jetty/documentation/9.1.1.v20140108/configuring-webapps.html#container-include-jar-pattern">the Jetty Documentation</a>)
	 */
	public final static String CONTAINER_INCLUDE_JAR_PATTERN = "webapp.ContainerIncludeJarPattern";

	
	/**
	 * The context path of the Web app. This property is required. It MUST start with a "/".
	 */
	public final static String PATH = "webapp.path";
	/**
	 * The server component this web app is supposed to run on. This property
	 * is required. The usual value is <code>environment/webServer</code>.
	 */
	public final static String SERVER = "webapp.server";

}
