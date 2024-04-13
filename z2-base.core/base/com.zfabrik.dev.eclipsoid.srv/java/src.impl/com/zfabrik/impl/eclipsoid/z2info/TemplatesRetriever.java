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
import java.util.Iterator;

import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.util.expression.X;

public class TemplatesRetriever extends AbstractRetriever {

	private final static String PROPKEY_COMPONENT_TEMPLATE_JSON = "com.zfabrik.eclipsoid.component.templates.json";
	
	private final static String TEMPLATE_FOR_PREFIX = "template.for.";
	
	public void provideInfoInto(JSONObject result) throws IOException {
		JSONObject templatesNode = new JSONObject();
		result.put("templates", templatesNode);

		// add templates defined in component factories
		Collection<String> allFactories = IComponentsManager.INSTANCE.findComponents(X.eq(var("com.zfabrik.component.type"), val("com.zfabrik.componentFactory")));
		for (String factoryComponent : allFactories) {

			IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(factoryComponent);
			
			// get the component type for the 
			String compType = cDesc.getProperty("componentFactory.type");
			if (compType == null) continue;
			
			// get the template-definition (it's  a JSON format)
			String json = cDesc.getProperty(PROPKEY_COMPONENT_TEMPLATE_JSON);
			addTemplate(templatesNode, compType, json);
			
		}
		
		// add additional templates defined by Eclipsoid
		IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent("com.zfabrik.dev.eclipsoid.srv/additionalTemplates");
		if (cDesc == null) return;
		
		for (Object propKey : cDesc.getProperties().keySet()) {
			String sKey = (String) propKey;
			if (sKey.startsWith(TEMPLATE_FOR_PREFIX)) {
				String compType = sKey.substring(TEMPLATE_FOR_PREFIX.length());
				String json = cDesc.getProperty(sKey);
				addTemplate(templatesNode, compType, json);
			}
		}
	}

	private void addTemplate(JSONObject result, String compType, String templateJson) {
		if (templateJson == null || templateJson.trim().length() == 0) return;
		
		JSONObject templateProps = new JSONObject(templateJson);

		String compTypeNodeName = compType + "?type=componentType";
		JSONObject compTypeNode = new JSONObject();
		result.put(compTypeNodeName, compTypeNode);
		
		Iterator<?> loopPropKeys = templateProps.keys();
		while (loopPropKeys.hasNext()) {
			String key = (String) loopPropKeys.next();
			Object value = templateProps.get(key);
			compTypeNode.put(key + "?type=property", value);
		}
	}
}
