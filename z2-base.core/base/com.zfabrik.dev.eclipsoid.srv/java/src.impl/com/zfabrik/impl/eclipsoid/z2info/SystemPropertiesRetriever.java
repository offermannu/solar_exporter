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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import org.json.JSONObject;

public class SystemPropertiesRetriever extends AbstractRetriever {

	private static final String SYSTEM_PROPERTIES = "systemProperties";

	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || SYSTEM_PROPERTIES.equalsIgnoreCase(type);
	}

	public void provideInfoInto(JSONObject result) throws IOException {
		JSONObject propsNode = new JSONObject();
		result.put(SYSTEM_PROPERTIES, propsNode);
		
		JSONObject z2Props = new JSONObject();
		propsNode.put("z2-Properties?type=folder", z2Props);

		JSONObject sysProps = new JSONObject();
		propsNode.put("System-Properties?type=folder", sysProps);

		for (Entry<Object, Object> prop : System.getProperties().entrySet()) {
			
			String key = (String) prop.getKey();
			String value = (String) (!isSensitiveKey(prop.getKey().toString())? prop.getValue() : "*****");
			
			if (key.startsWith("com.zfabrik") || key.startsWith("worker.")) {
				z2Props.put(key + "?type=property", value);
			} else {
				sysProps.put(key + "?type=property", value);
			}
		}
		
		JSONObject rtProps = new JSONObject();
		propsNode.put("Java Runtime?type=folder", rtProps);
		
		rtProps.put("name?type=property", ManagementFactory.getRuntimeMXBean().getName());
		rtProps.put("pid?type=property", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		rtProps.put("startTime?type=property", SimpleDateFormat.getDateTimeInstance().format(new Date(ManagementFactory.getRuntimeMXBean().getStartTime())));
//		rtProps.put("bootClassPath?type=property", ManagementFactory.getRuntimeMXBean().getBootClassPath());
		rtProps.put("vmName?type=property", ManagementFactory.getRuntimeMXBean().getVmName());
		rtProps.put("vmVendor?type=property", ManagementFactory.getRuntimeMXBean().getVmVendor());
		rtProps.put("vmVersion?type=property", ManagementFactory.getRuntimeMXBean().getVmVersion());

		JSONObject argsNode = new JSONObject();
		rtProps.put("Start Arguments", argsNode);
		List<String> startArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();

		for (int idx = 0; idx < startArgs.size(); idx++) {
			argsNode.put(String.format("%2d?type=property", idx), startArgs.get(idx));
		}
	}
}
