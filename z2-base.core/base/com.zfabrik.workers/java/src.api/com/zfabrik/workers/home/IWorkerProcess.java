/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.workers.home;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.StringTokenizer;

import com.zfabrik.work.ApplicationThreadPool;

/**
 * A worker process is managed by the home process as explained in {@link IHomeLayout}.
 * <p>
 * Worker process component definitions have no resources beyond a property sheet. A worker
 * process definition typically looks like this:
 * <pre>
 * com.zfabrik.component.type=com.zfabrik.worker
 * # worker configuration
 * #
 * # target states
 * #
 * worker.states=environment/webWorkerUp
 *
 * #
 * # max worker thread pool concurrency
 * #
 * worker.concurrency=15
 *
 *
 * #
 * # debug port
 * #
 * worker.debug.port=5001
 * #
 * # timeout (in ms) before the worker will be forcefully killed at worker stop
 * #
 * worker.process.timeouts.termination=20000
 * #
 * # vm options
 * #
 * worker.process.vmOptions=\
 *  -Xmx128m -Xms128m -XX:+HeapDumpOnOutOfMemoryError \
 *  -Duser.language=en \
 *  -Dcom.sun.management.jmxremote.port=7778
 * </pre>
 * <p>
 */
public interface IWorkerProcess {

	/**
	 * ({@value #VMOPTS})
	 * General virtual machine parameters for the worker process. See for example
	 * <a href="http://blogs.sun.com/watt/resource/jvm-options-list.html">http://blogs.sun.com/watt/resource/jvm-options-list.html</a> for a general overview.
	 * These params may be qualified further with the value of the system property "os.name" in the form of e.g.
	 * <pre>
	 * worker.process.vmOptions.Windows\ 7=...
	 * </pre>
	 * In the presence of the latter these will be considered with preference.
	 * <p>
	 * Note: Virtual machine parameters will be split by separator characters as in {@link StringTokenizer}. Test enclosed in double quotes will
	 * be considered a single token (embracing double quotes excluded).  
	 * If it is necessary to pass parameters containing a space character for example, use quotes to enclose the
	 * option. If the option in turn requires to specify quotes, escape those using the backslash \ character.
	 * <p>
	 * For example, 
	 * <pre>
	 * worker.process.vmOptions=-Dprop1=A -Dprop2=it works
	 * </pre>
	 * would resolve to system properties <code>prop1="A"</code>, <code>prop2="it"</code> and a probably unexpected option <code>works</code>.
	 * Using instead
	 * <pre>
	 * worker.process.vmOptions=-Dprop1=A "-Dprop2=it works"
	 * </pre>
	 * would resolve to system properties <code>prop1="A"</code>, <code>prop2="it works"</code> as desired.
	 * If we intend to get <code>prop2="it \"works\""</code>, i.e. the string (delimited with single quotes for clarity) 'it "works"', we would use
	 * <pre>
	 * worker.process.vmOptions=-Dprop1=A "-Dprop2=\"it works\""
	 * </pre>
	 * 
	 *
	 */
	String VMOPTS   = "worker.process.vmOptions";
	/**
	 * ({@value #STATES})
	 * Target states (or dependency components) of the worker process (see above).
	 * The worker process, when starting, will
	 * try to attain these target states (or prepare these dependency components) and will try so again
	 * during each verification and synchronization.
	 */
	String STATES   = "worker.states";

	/**
	 * ({@value #TO_START})
	 * Timeout in milliseconds. This time out determines when the worker process implementation
	 * will forcibly kill the worker process if it has not reported startup completion until then.
	 */
	String TO_START = "worker.process.timeouts.start";
	/**
	 * ({@value #TO_TERM})
	 * Timeout in milliseconds. This time out determines when the worker process implementation
	 * will forcibly kill the worker process if it has not terminated after this timeout has
	 * passed since it was asked to terminate.
	 */
	String TO_TERM  = "worker.process.timeouts.termination";
	/**
	 * ({@value #TO_MSG})
	 * Timeout in milliseconds. This time out is the default timeout that determines the
	 * time passed after which the worker
	 * process implementation will forcibly kill a worker process if a message request
	 * has not returned.
	 */
	String TO_MSG   = "worker.process.timeouts.communication";

	/**
	 * ({@value #WORKER_CONCURRENCY})
	 * Size of application thread pool (see {@link ApplicationThreadPool}). In general this thread
	 * pool is used for application type work (e.g. for web requests or parallel execution within
	 * the application). This property helps achieving a simple but effective
	 * concurrent load control
	 */
	String WORKER_CONCURRENCY = "worker.concurrency";


	/**
	 * ({@value #WORKER_DEBUG})
	 * Debugging for the worker process will be configured if this system property is set to
	 * <code>true</code> in the home process, e.g. by setting "-Dworker.debug=true" in the home
	 * process command line.
	 * <p>
	 * This is not a profile parameter for the worker process
	 * definition. This is strictly the method to enable debugging for worker processes as launched
	 * by the home process.
	 * Otherwise the worker process will not be configured for debugging.
	 */
	String WORKER_DEBUG = "worker.debug";


	/**
	 * ({@value #DEBUG_PORT})
	 * The debug port to use for this worker process. If this configuration is present and the home process is configured
	 * for debugging, so will the worker process.
	 * This port is subject to variant computation in the presence of detached worker processes. That is, this base port number will be increased by the variant number of the
	 * worker process that increases with every added instance, starting at 0. The variant number is the &commat;-separated tail of the worker process name.
	 * See {@link #DEBUG_PARAMS} for actual parameters added to the worker process command line.
	 */
	String DEBUG_PORT   = "worker.debug.port";

	/**
	 * ({@value #DEBUG_PARAMS})
	 * In case debugging is enabled (see {@link #WORKER_DEBUG} the params specified by this property
	 * will be added. These default to
	 * <pre>
	 * {@value #DEBUG_PARAMS_DEFAULT}
	 * </pre>
	 * The substitution {0} will be replaced by a debug port computed as in {@link #DEBUG_PORT}.
	 */
	String DEBUG_PARAMS = "worker.debug.params";

	/**
	 * ({@value #DEBUG_PARAMS_DEFAULT})
	 * Default value for {@link #DEBUG_PARAMS}.
	 */
	String DEBUG_PARAMS_DEFAULT = "-Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,suspend=n,server=y,address={0}";

	/**
	 * ({@value #CLASSPATH})
	 * Classpath setting for the worker process. By default, the worker process inherits the class path from the home process.
	 * If that is not desired this setting allows to specify a different classpath. All paths should be specified relative to
	 * the current working folder which is typically the ${Z2_HOME}/bin folder.
	 * If the property value has a substitution field {0} it will be replaced by the home process classpath
	 */
	String CLASSPATH = "worker.class.path";

	/**
	 * ({@value #INHERITED_SYSPROPS})
	 * A comma-separated list of system properties for the worker process. The value of some system properties may be forwarded on to the
	 * worker process. Currently this defaults to {@value #INHERITED_SYSPROPS_DEFAULT}
	 */
	String INHERITED_SYSPROPS = "worker.inheritedSystemProperties";

	/**
	 * Default value of {@link #INHERITED_SYSPROPS}.
	 */
	String INHERITED_SYSPROPS_DEFAULT =
			"com.sun.management.config.file,com.zfabrik.config,java.util.logging.config.file,com.zfabrik.dev.local.workspace,com.zfabrik.dev.local.repo,"+
			"com.zfabrik.repo.mode,com.zfabrik.proxy.auth,java.library.path,com.zfabrik.mode,"+
			"https.proxyPort,https.proxyHost,http.proxyPort,http.proxyHost,proxyPort,proxyHost,proxyUser,proxyPassword";


	/**
	 * ({@value #WORKER_REMOTE_JMX})
	 * JMX remote access for the worker process will be configured if this system property is set to
	 * <code>true</code> in the home process, e.g. by setting "-Dworker.remoteJMX=true" in the home
	 * process command line.
	 * <p>
	 * This is not a profile parameter for the worker process
	 * definition. This is strictly the method to enable remote JMX port setting for worker processes as launched
	 * by the home process.
	 */
	String WORKER_REMOTE_JMX = "worker.remoteJmx";

	/**
	 * ({@value #JMX_PORT})
	 * The jmx port to use for this worker process. If this is set, the system property "com.sun.management.jmxremote.port" will be set via a system property declaration
	 * in the worker process command line.
	 * This port is subject to variant computation in the presence of detached worker processes. That is, this base port number will be increased by the variant number of the
	 * worker process that increases with every added instance, starting at 0. The variant number is the &commat;-separated tail of the worker process name.
	 */
	String JMX_PORT   = "worker.jmx.port";

	/**
	 * ({@value #JMX_PARAMS})
	 * In case remote JMX access is enabled (see {@link #WORKER_REMOTE_JMX} the params specified by this property
	 * will be added. These default to
	 * <pre>
	 * {@value #JMX_PARAMS_DEFAULT}
	 * </pre>
	 * The substitution {0} will be replaced by a debug port computed as in {@link #JMX_PORT}.
	 */
	String JMX_PARAMS = "worker.jmx.params";

	/**
	 * ({@value #JMX_PARAMS_DEFAULT})
	 * Default value for {@link #JMX_PARAMS}.
	 */
	String JMX_PARAMS_DEFAULT = "-Dcom.sun.management.jmxremote.port={0}";

	// long start timeout to account for remote download
	final static String TO_START_DEF = "600000";
	final static String TO_TERM_DEF = "30000";
	final static String TO_MSG_DEF = "60000";



	/**
	 * msg header describing the target of a message
	 */
	final static String MSG_TARGET = "msg.target";
	final static String MSG_TARGET_NODE = "s";
	final static String MSG_TARGET_ALL = "w";
	final static String MSG_TARGET_HOME = "h";

	/**
	 * msg header providing id of target node if one specific
	 */
	final static String MSG_NODE = "msg.node";

	/**
	 * msg header providing the time out of the message operation (per node).
	 * Can be provided as long or as string
	 */
	final static String MSG_TIMEOUT = "msg.timeout";

	final static short NOT_STARTED  = 0;
	final static short STARTING 	= 1;
	final static short STARTED  	= 2;
	final static short DETACHED  	= 3;
	final static short STOPPING 	= 4;

	/**
	 * send a message to the process. Note: message sending happens completely
	 * synchronously. A message is just a name value map. All content must be
	 * serializable (preferrably string). Its return value is also just a map.
	 *
	 * @param args
	 *            a map of name value pairs that will be serialized as strings
	 *            to the slave
	 * @param timeout
	 *            timeout for message answering in ms.
	 * @return a result map
	 * @throws IOException
	 */
	Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout) throws IOException;

	/**
	 * send a message to the process. Note: message sending happens completely
	 * synchronously. A message is just a name value map. All content must be
	 * serializable (preferrably string). Its return value is also just a map.
	 *
	 * @param args
	 *            a map of name value pairs that will be serialized as strings
	 *            to the slave
	 * @param timeout
	 *            timeout for message answering in ms.
	 * @param killOnTimeOut if <code>true</code> a timeout will lead to a process termination.
	 * @return a result map
	 * @throws IOException
	 */
	Map<String, Serializable> sendMessage(Map<String, Serializable> args, long timeout,boolean killOnTimeOut) throws IOException;

	/**
	 * Stop this worker
	 */
	void stop();

	/**
	 * Stop this worker with a non-default timeout. A timeout greater than 0 will ask the worker process
	 * to terminate gracefully. A timeout of 0 will stop the process immediately
	 */
	void stop(long timeout);


	/**
	 * start this worker
	 */
	void start();

	/**
	 * Detach this worker. Detaching means that this worker will no longer receive synchronization notifications from
	 * the home process. This worker is meant to complete its work and terminate gracefully.
	 */
	void detach();

	/**
	 * Returns whether the current process is running. This is equivalent to being the latest and up or up and detached.
	 */
	boolean isRunning();

	/**
	 * get the state of this worker
	 */
	short getState();

	/**
	 * Get the worker process' component name
	 */
	String getComponentName();

	/**
	 * Get the worker process's instance technical name
	 */
	String getName();

	/**
	 * Get the creation date of the worker process object (this is not necessarily the time it got started)
	 */
	long getCreated();

	/**
	 * Get the time of detachment (if any). Returns &lt;0 if not detached.
	 */
	long getDetachTime();
}
