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

import static com.zfabrik.impl.eclipsoid.z2info.DependenciesRetriever.component;
import static com.zfabrik.impl.eclipsoid.z2info.DependenciesRetriever.module;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Properties;

import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.util.expression.X;

public class ProjectsRetriever extends AbstractRetriever {

	private static final String PROJECTS = "projects";

	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || PROJECTS.equalsIgnoreCase(type);
	}

	public void provideInfoInto(JSONObject result) throws IOException {
		JSONObject projectsNode = new JSONObject();
		result.put("projects", projectsNode);
		
		Collection<String> allComps = IComponentsManager.INSTANCE.findComponents(X.val(true));
		for (String component : allComps) {

			IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(component);

			// assume component is always <project-name>/<component-name>
			String projectName = module(component);
			String compName = component(component);
			
			addEntry(projectsNode, projectName, compName, cDesc.getProperties());
		}
		
	}
	
	private void addEntry(JSONObject projectsNode, String projectName, String compName, Properties props) {
		JSONObject compsNode;
		String projectsNodeName = projectName + "?type=project";
		if (projectsNode.has(projectsNodeName)) {
			compsNode = projectsNode.getJSONObject(projectsNodeName);
		} else {
			compsNode = new JSONObject();
			projectsNode.put(projectsNodeName, compsNode);
		}
		
		JSONObject propsNode;
		String compsNodeName = compName + "?type=component";
		if (compsNode.has(compsNodeName)) {
			propsNode = compsNode.getJSONObject(compsNodeName);
		} else {
			propsNode = new JSONObject();
			compsNode.put(compsNodeName, propsNode);
		}
		
		for (Entry<Object, Object> prop : props.entrySet()) {
			if (! isSensitiveKey(prop.getKey().toString())) {
				propsNode.put(prop.getKey() + "?type=property", prop.getValue());
			} else {
				propsNode.put(prop.getKey() + "?type=property", "*****");
			}
		}
	}
	
}
