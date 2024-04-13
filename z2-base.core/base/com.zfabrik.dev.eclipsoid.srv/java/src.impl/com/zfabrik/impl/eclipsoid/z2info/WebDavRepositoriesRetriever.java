/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.z2info;

import static com.zfabrik.util.expression.X.val;
import static com.zfabrik.util.expression.X.var;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.provider.IComponentsRepository;
import com.zfabrik.util.expression.X;

public class WebDavRepositoriesRetriever extends AbstractRetriever {

	//	private final static String SERVER = "http://192.168.0.106:8001";
	//	private final static String SERVER = "file:.";
	//	private final static String REPO_ROOT = "/sap/bc/bsp_dev/SAP/ZBSP1";
	//	private final static String PROJECT = "com.zfabrik.dev.eclipsoid.srv";

	private final static String REPOSITORIES = "repositories";

	private final static Logger logger = Logger.getLogger(WebDavRepositoriesRetriever.class.getName());

	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || REPOSITORIES.equalsIgnoreCase(type);
	}

	public void provideInfoInto(JSONObject result) throws IOException {

		if (! result.has(REPOSITORIES)) {
			result.put(REPOSITORIES, new JSONObject());
		}

		JSONObject reposNode = result.getJSONObject(REPOSITORIES);

		Collection<String> allComps = IComponentsManager.INSTANCE.findComponents(X.val(true));

		Collection<String> allRepoComps = IComponentsManager.INSTANCE.findComponents(X.eq(var("com.zfabrik.component.type"), val("com.zfabrik.davcr")));
		for (String repoComp : allRepoComps) {

			IComponentsRepository davcr = IComponentsLookup.INSTANCE.lookup(repoComp, IComponentsRepository.class);
			if (davcr == null) {
				logger.warning("Cannot instantiate Dav-Component-Repository '" + repoComp + "'.");
				continue;
			}

			IComponentDescriptor davDesc = IComponentsManager.INSTANCE.getComponent(repoComp);

			String davUrl = davDesc.getProperty("davcr.url");
			if (davUrl == null || davUrl.length() == 0) {
				throw new IOException("Missing property 'davcr.url' in DAV-CR Component " + repoComp);
			}

			JSONObject compsNode = new JSONObject();
			reposNode.put(davUrl + "?crName=" + repoComp + "&crType=com.zfabrik.davcr&crUrl=" + davUrl, compsNode);

			for (String component : allComps) {
				String projectName = DependenciesRetriever.module(component); // component: <project-name>/<component-name> - cut off the components part

				// handle each project only ones
				if (compsNode.has(projectName)) continue;

				// is this component available in the current repository?
				try {
					IComponentDescriptor compDesc = davcr.getComponent(component);
					String realRepoComponent = (compDesc != null)? compDesc.getProperty(IComponentsRepository.COMPONENT_REPO_IMPLEMENTATION) : null;

					if (repoComp.equals(realRepoComponent)) {
						compsNode.put(projectName, "/" + projectName);
					}
				} catch (Exception e) {
					logger.log(Level.WARNING, "Cannot access Dav-Component-Repository '" + repoComp + "'.", e);
				}
			}
		}		
	}
}
