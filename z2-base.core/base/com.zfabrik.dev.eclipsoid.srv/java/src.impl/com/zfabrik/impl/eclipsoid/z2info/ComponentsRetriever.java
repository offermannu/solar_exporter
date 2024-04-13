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

import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.util.expression.X;

public class ComponentsRetriever extends AbstractRetriever {

	private static final String COMPONENTS = "components";

	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || COMPONENTS.equalsIgnoreCase(type);
	}
	
	public void provideInfoInto(JSONObject result) throws IOException {

		JSONObject compTypeNode = new JSONObject();
		result.put("components", compTypeNode);
		
		Collection<String> allComps = IComponentsManager.INSTANCE.findComponents(X.val(true));
		for (String component : allComps) {

			IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(component);
			String compType = cDesc.getType();

			if (compType == null) continue;

			JSONObject projectsNode;
			String compTypeNodeName = compType + "?type=componentType";
			if (compTypeNode.has(compTypeNodeName)) {
				projectsNode = compTypeNode.getJSONObject(compTypeNodeName);
				
			} else {
				projectsNode = new JSONObject();
				compTypeNode.put(compTypeNodeName, projectsNode);
			}
			
			// assume component is always <project-name>/<component-name>
			String projectName = module(component);
			String compName = component(component);
			
			JSONObject compsNode;
			String projectNodeName = projectName + "?type=project";
			if (projectsNode.has(projectNodeName)) {
				compsNode = projectsNode.getJSONObject(projectNodeName);
			} else {
				compsNode = new JSONObject();
				projectsNode.put(projectNodeName, compsNode);
			}

			JSONObject propsNode;
			String compsNodeName = compName + "?type=component";
			if (compsNode.has(compsNodeName)) {
				propsNode = compsNode.getJSONObject(compsNodeName);
			} else {
				propsNode = new JSONObject();
				compsNode.put(compsNodeName, propsNode);
			}
			
			for (Entry<Object, Object> prop : cDesc.getProperties().entrySet()) {
				if (! isSensitiveKey(prop.getKey().toString())) {
					propsNode.put(prop.getKey() + "?type=property", prop.getValue());
				} else {
					propsNode.put(prop.getKey() + "?type=property", "*****");
				}
			}
		}
		
		// add components defined by componentFactory which are not added yet
		Collection<String> compFactories = IComponentsManager.INSTANCE.findComponents(X.val("com.zfabrik.componentFactory").eq(X.var("com.zfabrik.component.type")));
		for (String cf : compFactories) {
			IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(cf);
			
			String compType = cDesc.getProperty("componentFactory.type");
			JSONObject projectsNode;
			String compTypeNodeName = compType + "?type=componentType";
			if (!compTypeNode.has(compTypeNodeName)) {
				projectsNode = new JSONObject();
				compTypeNode.put(compTypeNodeName, projectsNode);
			}


		}
	}
}
