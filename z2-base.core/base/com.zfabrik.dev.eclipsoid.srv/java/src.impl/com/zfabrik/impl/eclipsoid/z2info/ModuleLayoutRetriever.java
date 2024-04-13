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

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.zfabrik.components.IComponentsLookup;

public class ModuleLayoutRetriever extends AbstractRetriever {

	@Override
	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || "moduleLayout".equals(type);
	}
	
	@Override
	public void provideInfoInto(JSONObject result) throws IOException {
		JSONObject moduleLayoutJSON = new JSONObject();
		result.put("moduleLayout", moduleLayoutJSON);
		
		File modulesDir = IComponentsLookup.INSTANCE.lookup("com.zfabrik.dev.eclipsoid.srv/moduleLayouts", File.class);
		if (modulesDir != null) {
			
			File[] jsonFiles = modulesDir.listFiles(new FileFilter() {
				public boolean accept(File f) {
					return f.isFile() && f.getName().endsWith(".json");
				}
			});
			
			for (File jf : jsonFiles) {
				String moduleName = jf.getName().substring(0, jf.getName().length() - ".json".length());
				String content = new Scanner(new FileReader(jf)).useDelimiter("\\Z").next();
				JSONArray moduleJson = new JSONArray(new JSONTokener(content));
				moduleLayoutJSON.put(moduleName, moduleJson);
			}
		}
		
	}

}
