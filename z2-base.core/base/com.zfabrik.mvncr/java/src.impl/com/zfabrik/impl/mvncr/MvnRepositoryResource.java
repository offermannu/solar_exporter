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

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.components.provider.IComponentsRepository;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;

/**
 * A component repository implementation component using maven repositories. The repository is configured by specifying remote repositories
 * in a standard maven settings.xml file and by listing roots to resolve from recursively in a component descriptor properties
 * file.
 * <p/>
 * Starting from its configured roots the repository will expose Java components having the jar artifact as API library and having its maven references
 * of the configured scope as public refs. Components will be named using the artifacts <code>groupId</code> and <code>artifactId</code>. By default the artifact version will be omitted. 
 * In case a combination of <code>groupId</code> and <code>artifactId</code> is encountered with different versions the higher version will be considered. If an artifact version 
 * is specified via managed artifact, only the specified version will be considered.
 * <p/>
 * Depending on the root configuration however (see below) versioned component names may be forced to support exceptional use of multiple artifact versions.
 * <p/>
 * Note: Only for truly independent libraries this is the right approach out of the box. Typically, it will be adivsed to include an artifact with or 
 * without its transitive closure into another z2 component.
 * <h2>Configuration Properties</h2>
 * Using a component descriptor the following properties can be set
 * <table border="1">
 * <tr><th>name</th><th>meaning</th><th>default</th></tr>
 * <tr><td>mvncr.settings</td><td>Specifies the location of the settings XML file relative to the components resources</td><td>settings.xml</td></tr>
 * <tr><td>mvncr.roots</td><td>A comman-separated list of root artifacts. See below for more details</td><td>n.a.</td></tr>
 * <tr><td>mvncr.priority</td><td>The repository priority in repository chaining as defined in {@link IComponentsRepository}</td><td>500</td></tr>
 * <tr><td>mvncr.managed</td><td>Fixed artifact versions, if encountered during recursive root resolution. This corresponds to a &lt;dependencyManagement&gt; section in a Maven POM file</td><td>n.a.</td></tr>
 * <tr><td>mvncr.excluded</td><td>A comma separated list of artifacts that will be skipped during resolution of any root</td><td>n.a.</td></tr>
 * <tr><td>mvncr.repository</td><td>The symbolic name of the MVNCR repository. Fragments use this name to refer to. Defaults to the component name.</td><td> n.a. </td></tr>
 * </table>
 * 
 * <h2>Artifact Naming and Modifiers</h2>
 * All artifacts for use in <code>mvncr.roots</code> or <code>mvncr.managed</code> are specified in coordinates as defined in {@link ArtifactName}.
 * <p/>
 * Artifacts specified in <code>mvncr.roots</code> may be further qualified by a query string as for example in
 * <pre>
 * org.springframework:spring-context:4.0.2.RELEASE?scope=RUNTIME&versioned=true
 * </pre>
 * The following modifiers are supported:
 * <table border="1">
 * <tr><th>name</th><th>meaning</th><th>default</th></tr>
 * <tr><td>scope</td><td>Maven scope to resolve for. Resolution is always recursive and only considers non-optional dependencies</td><td>COMPILE</td></tr>
 * <tr><td>versioned</td><td>When set to <code>false</code>, the repository will provide unversioned component name. If set to <code>true</code>, versioned components will be created and for those no version conflict resolution will be applied nor will managed artifacts have any impact on these.</td><td><code>false</code></td></tr>
 * <tr><td>excluded</td><td>A comma separated list of artifacts that will be skipped during resolution</td><td>n.a.</td></tr>
 * </table>
 * <h2>Example</h2>
 * An example settings.xml that allows dependency and artifact retrieval from maven central looks like this:
 * <pre>
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;settings xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd"
 *     xmlns="http://maven.apache.org/SETTINGS/1.0.0"
 *     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"&gt;
 *    &lt;profiles&gt;
 *        &lt;profile&gt;
 *            &lt;repositories&gt;
 *                &lt;repository&gt;
 *                    &lt;id&gt;central&lt;/id&gt;
 *                    &lt;name&gt;central&lt;/name&gt;
 *                    &lt;releases&gt;
 *                    		&lt;enabled&gt;true&lt;/enabled&gt;
 *                    		&lt;updatePolicy&gt;never&lt;/updatePolicy&gt;
 *                    		&lt;checksumPolicy&gt;warn&lt;/checksumPolicy&gt;
 *                    &lt;/releases&gt;
 *                    &lt;snapshots&gt;
 *                    		&lt;enabled&gt;true&lt;/enabled&gt;
 *                    		&lt;updatePolicy&gt;daily&lt;/updatePolicy&gt;
 *                    		&lt;checksumPolicy&gt;fail&lt;/checksumPolicy&gt;
 *                    &lt;/snapshots&gt;
 *                    &lt;url&gt;http://central.maven.org/maven2/&lt;/url&gt;
 *                    &lt;layout&gt;default&lt;/layout&gt;
 *                &lt;/repository&gt;
 *            &lt;/repositories&gt;
 *        &lt;/profile&gt;
 *    &lt;/profiles&gt;
 * &lt;/settings&gt;
 * </pre>
 * A component declaration for a maven based component repository would then look like this:
 * <pre>
 * com.zfabrik.systemStates.participation=com.zfabrik.boot.main/sysrepo_up
 * com.zfabrik.component.type=com.zfabrik.mvncr
 * mvn.priority=200
 * 
 * mvn.roots=\
 * 	org.springframework:spring-context:4.0.2.RELEASE,\
 * 	org.springframework:spring-aspects:4.0.2.RELEASE,\
 * 	org.springframework:spring-tx:4.0.2.RELEASE,\
 * 	org.springframework:spring-orm:4.0.2.RELEASE,\
 * 	org.springframework:spring-web:4.0.2.RELEASE
 *  
 * mvn.managed=\
 * 	commons-logging:commons-logging:jar:1.1.2
 * </pre>
 * 
 * This would imply Java components such as <code>org.springframework:spring-context</code>. While the Spring Framework of version 4.0.2.RELEASE depends on version 1.1.3 of the
 * commons-logging framework, we explicitly chose to only include version 1.1.2.
 * 
 * 
 * @author hb
 *
 */
public class MvnRepositoryResource extends Resource implements IDependencyComponent {
	/**
	 * Component Type
	 */
	public final static String TYPE = "com.zfabrik.mvncr";
	
	private String name; 
	private MvnRepositoryImpl repo;
	
	public MvnRepositoryResource(String name) {
		this.name = name;
	}

	@Override
	public <T> T as(Class<T> clz) {
		if (MvnRepositoryImpl.has(clz)) {
			_load();
			return this.repo.as(clz);
		} 
		if (IDependencyComponent.class.equals(clz)) {
			return clz.cast(this);
		}
		return super.as(clz);
	}
	
	private synchronized void _load() {
		if (this.repo == null) {
			try {
				this.repo = new MvnRepositoryImpl(
					this.name,
					handle().as(IComponentDescriptor.class).getProperties()
				);
				this.repo.start();
			} catch (Exception e) {
				throw new RuntimeException("Failed to initialize mvn repo: "+name,e);
			}
		}
	}

	@Override
	public void invalidate() throws ResourceBusyException {
		try {
			if (this.repo!=null) {
				this.repo.stop();
			}
		} finally {
			this.repo = null;
		}
	}
	
	@Override
	public void prepare() {
		_load();
	}
}
