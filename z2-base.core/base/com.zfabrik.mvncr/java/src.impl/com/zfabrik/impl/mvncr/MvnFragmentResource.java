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

import java.util.Properties;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.resources.provider.Resource;

/**
 * A fragment for a Maven based component repository (see {@link MvnRepositoryResource}). An mvncr fragment adds configuration information to an mvncr repository by referring
 * to it and specifying configuration properties that can be merged.
 * <p/>
 * The purpose of fragments is to support modularization of mvncr component repository configuration while still maintaining one Maven dependency graph. See also <a href="https://redmine.z2-environment.net/projects/z2-environment/wiki/Maven_repo_support">the wiki</a>.
 *
 * <h2>Configuration Properties</h2>
 * Using a component descriptor the following properties can be set
 * <table border="1">
 * <tr><th>name </th><th>meaning </th><th>default </th></tr>
 * <tr><td>mvncr.component</td><td> The component name (or a comma-separated list of component names) of MVNCR declarations this fragment adds to or a fragment adding to some other mvncr. <b>Deprecated</b> as component names are not robust for this use-case when using links. Instead use mvncr.repository</td><td> n.a. </td></tr>
 * <tr><td>mvncr.repository</td><td>The symbolic name (or a comma-separated list of symbolic names) of MVNCR declarations this fragment adds to or a fragment adding to some other mvncr.</td><td> n.a. </td></tr>
 * <tr><td>mvncr.roots </td><td> As above. Will be merged with the repo this fragment adds to </td><td> n.a. </td></tr>
 * <tr><td>mvncr.managed</td><td> As above. Will be merged with the repo this fragment adds to </td><td> n.a. </td></tr>
 * <tr><td>mvncr.excluded </td><td> As above. Will be merged with the repo this fragment adds to </td><td> n.a. </td></tr>
 * </table>
 * 
 * 
 * @author hb
 *
 */
public class MvnFragmentResource extends Resource {
	/**
	 * Component Type
	 */
	public final static String TYPE = "com.zfabrik.mvncr.fragment";
	
	/**
	 * The component name (or a comma-separated list of component names) of MVNCR declarations this fragment adds to or a fragment adding to some other mvncr.
	 * @deprecated Use {@link MvnRepositoryImpl#MVN_REPOSITORY} instead
	 */
	public static final String MVNCR_COMPONENT = "mvncr.component";

	
	private String name;

	
	public MvnFragmentResource(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	@Override
	public <T> T as(Class<T> clz) {
		if (Properties.class.equals(clz)) {
			return clz.cast(handle().as(IComponentDescriptor.class).getProperties());
		}
		return super.as(clz);
	}
	
}
