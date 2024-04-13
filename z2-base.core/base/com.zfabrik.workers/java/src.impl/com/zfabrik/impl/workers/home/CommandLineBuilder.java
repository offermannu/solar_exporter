/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.workers.home;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.zfabrik.util.runtime.Foundation;
import com.zfabrik.workers.home.IWorkerProcess;

/**
 * A utility to build a worker process command line from various sources.
 */
public class CommandLineBuilder {
	public static final String WORKER_LAUNCHER = "com.zfabrik.launch.impl.WorkerLauncher";

	private final static Logger LOG = Logger.getLogger(CommandLineBuilder.class.getName());

	private Integer variantNum;
	private String classPath;
	private List<String> args = new LinkedList<>();

	public void setClassPath(String classPath) {
		this.classPath = classPath;
	}

	public void addRawOptions(String options) {
		args.addAll(CommandLineParser.split(options));
	}

	public boolean addSysProperty(String name, String value, boolean mandatory) {
		if (value != null) {
			args.add("-D"+name+"="+value);
			return true;
		} else {
			if (mandatory)
				throw new IllegalArgumentException("No value provided for system property " + name+" while configuring the worker process");
			return false;
		}
	}

	public CommandLineBuilder(String componentName, int variantNum) {
		this.variantNum = variantNum;
	}


	/**
	 * Factory using the worker process configuration
	 */
	public static CommandLineBuilder fromConfig(String componentName, int variantNum, Properties props) {
		CommandLineBuilder b = new CommandLineBuilder(componentName, variantNum);

		// 1. classpath
		String cp = props.getProperty(IWorkerProcess.CLASSPATH);
		if (cp!=null && (cp=cp.trim()).length()>0) {
			cp = MessageFormat.format(cp, System.getProperty("java.class.path"));
		} else {
			cp = System.getProperty("java.class.path");
		}
		b.setClassPath(cp);

		// 2. debug
		// only if home has the flag and the port is set
		if (Boolean.getBoolean(IWorkerProcess.WORKER_DEBUG)) {
			// compute debug port
			Integer p = b.computeVariantPort(props.getProperty(IWorkerProcess.DEBUG_PORT));
			if (p!=null) {
				// ok. now compute the debug params
				String dp = props.getProperty(IWorkerProcess.DEBUG_PARAMS, IWorkerProcess.DEBUG_PARAMS_DEFAULT);
				if (dp!=null && (dp=dp.trim()).length()>0) {
					dp = MessageFormat.format(dp, p.toString());
				}
				b.addRawOptions(dp);
			} else {
				LOG.warning("Workers suggested to be in debug mode by home setting, but no debug port configured: "+componentName);
			}
		}

		// 3. JMX
		if (Boolean.getBoolean(IWorkerProcess.WORKER_REMOTE_JMX)) {
			Integer jmxp = b.computeVariantPort(props.getProperty(IWorkerProcess.JMX_PORT));
			if (jmxp!=null) {
				// ok. now compute the debug params
				String dp = props.getProperty(IWorkerProcess.JMX_PARAMS, IWorkerProcess.JMX_PARAMS_DEFAULT);
				if (dp!=null && (dp=dp.trim()).length()>0) {
					dp = MessageFormat.format(dp, jmxp.toString());
				}
				b.addRawOptions(dp);
			}
		}

		// 4. pass on home sysprops
		String sp = props.getProperty(IWorkerProcess.INHERITED_SYSPROPS,IWorkerProcess.INHERITED_SYSPROPS_DEFAULT);
		if (sp!=null && (sp=sp.trim()).length()>0) {
			for (String p : sp.split(",")) {
				if (p!=null && (p=p.trim()).length()>0) {
					b.addSysProperty(p, System.getProperty(p), false);
				}
			}
		}

		// 5. now add the actual command line string
		String value = props.getProperty(IWorkerProcess.VMOPTS+"."+System.getProperty("os.name"));
		if (value==null) {
			value = props.getProperty(IWorkerProcess.VMOPTS);
		}
		if (value != null) {
			b.addRawOptions(value);
		}


		// 5. the worker component
		b.addSysProperty(Foundation.PROCESS_WORKER, componentName+"@"+variantNum, true);
		
		// 6. the instance id (matching our home instance)
		b.addSysProperty(Foundation.INSTANCE_ID, Foundation.getInstanceId(), true);
		
		
		// 7. and the actual class name
		b.addRawOptions(WORKER_LAUNCHER);

		// that's it.
		return b;
	}

	private Integer computeVariantPort(String base) {
		if (base!=null) {
			// compute debug port
			int dp = Integer.parseInt(base.trim());
			dp += variantNum;
			return dp;
		}
		return null;
	}


	public String[] toStringArray() {
		if (classPath==null) {
			throw new IllegalStateException("No classpath specified for worker process");
		}
		List<String> res = new ArrayList<>(args.size()+10);
		res.add("java");
		// set classpath
		res.add("-cp");
		res.add(classPath);
		// add all other params
		res.addAll(args);
		return res.toArray(new String[res.size()]);
	}

}
