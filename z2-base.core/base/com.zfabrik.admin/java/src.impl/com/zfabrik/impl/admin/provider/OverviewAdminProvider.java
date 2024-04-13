/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.provider;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

import com.zfabrik.impl.admin.web.vm.InfoBean;
import com.zfabrik.management.home.IHomeMBeanServer;

public class OverviewAdminProvider implements IAdminProvider {
	public static final String CONNECTION_EXPIRATION = "ConnectionExpiration";
	public static final String NUM_SPARE_CONNECTIONS = "NumSpareConnections";
	public static final String NUM_IN_USE_CONNECTIONS = "NumInUseConnections";
	public static final String NUM_CONNECTIONS_SERVED = "NumConnectionsServed";
	public static final String NUM_CONNECTIONS_CREATED = "NumConnectionsCreated";
	public static final String MAX_IN_USE_CONNECTIONS = "MaxInUseConnections";
	public static final String MAX_CONCURRENT_CONNECTIONS = "MaxConcurrentConnections";
	public static final String EXPIRED_CONNECTIONS = "ExpiredConnections";
	public static final String TASKS_COMPLETED = "TasksCompleted";
	public static final String QUEUE_LENGTH = "QueueLength";
	public static final String MAX_QUEUE_LENGTH = "MaxQueueLength";
	public static final String MAX_CONCURRENCY_REACHED = "MaxConcurrencyReached";
	public static final String MAX_CONCURRENCY_ALLOWED = "MaxConcurrencyAllowed";
	public static final String SESSIONS = "Sessions";
	public static final String RESPONSES5XX = "Responses5xx";
	public static final String RESPONSES4XX = "Responses4xx";
	public static final String RESPONSES3XX = "Responses3xx";
	public static final String RESPONSES2XX = "Responses2xx";
	public static final String RESPONSES1XX = "Responses1xx";
	public static final String REQUESTS_TIME_MAX = "RequestTimeMax";
	// public static final String REQUESTS_TIME_MIN = "RequestTimeMin";
	public static final String REQUESTS_TIME_AVE = "RequestTimeAve";
	public static final String REQUESTS_ACTIVE_MAX = "RequestsActiveMax";
	public static final String REQUESTS_ACTIVE = "RequestsActive";
	public static final String REQUESTS = "Requests";
	public static final String COLLECTION_TIME = "CollectionTime";
	public static final String COLLECTION_COUNT = "CollectionCount";
	public static final String UNLOADED_CLASS_COUNT = "UnloadedClassCount";
	public static final String TOTAL_LOADED_CLASS_COUNT = "TotalLoadedClassCount";
	public static final String LOADED_CLASS_COUNT = "LoadedClassCount";
	public static final String TOTAL_STARTED_THREAD_COUNT = "TotalStartedThreadCount";
	public static final String PEAK_THREAD_COUNT = "PeakThreadCount";
	public static final String THREAD_COUNT = "ThreadCount";
	public static final String FREE_SWAP_SPACE_SIZE = "FreeSwapSpaceSize";
	public static final String TOTAL_SWAP_SPACE_SIZE = "TotalSwapSpaceSize";
	public static final String OPEN_FILE_DESCRIPTOR_COUNT = "OpenFileDescriptorCount";
	public static final String MAX_FILE_DESCRIPTOR_COUNT = "MaxFileDescriptorCount";
	public static final String FREE_PHYSICAL_MEMORY_SIZE = "FreePhysicalMemorySize";
	public static final String TOTAL_PHYSICAL_MEMORY_SIZE = "TotalPhysicalMemorySize";
	public static final String PROCESS_CPU_TIME = "ProcessCpuTime";
	public static final String VERSION = "Version";
	public static final String AVAILABLE_PROCESSORS = "AvailableProcessors";
	public static final String VM_VERSION = "VmVersion";
	public static final String VM_VENDOR = "VmVendor";
	public static final String VM_NAME = "VmName";
	public static final String UPTIME = "Uptime";
	public static final String NAME = "Name";
	public static final String MAX_SESSIONS = "MaxSessions";

	public static final String START_TIME = "StartTime";

	public static final String NUM_ERRORED_TIMERS = "NumberErroredTimers";
	public static final String NUM_TIMERS = "NumberTimers";
	public static final String NUM_ZOMBIE_TIMERS = "NumberZombieTimers";
	public static final String NUM_LOCKED_TIMER = "NumberLockedTimers";
	public final static long SEC = 1000;
	public final static long MIN = 60 * SEC;
	public final static long HOUR = 60 * MIN;
	public final static long DAY = 24 * HOUR;

	
	private IAdminProviderContext ctxt;
	
	@Override
	public void action(IHomeMBeanServer mbs, Map<String, String> params, List<String> messages) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void addBeans(IHomeMBeanServer mbs, List<InfoBean> beans) throws Exception {
		InfoBean b;
		Map<String, Object> al;
		Set<ObjectName> ons;
		// process
		b = new InfoBean("Process Overview");
		al = ctxt.getAsMap(mbs.getAttributes(ObjectName.getInstance("java.lang:type=Runtime"), new String[] { START_TIME, UPTIME, VM_NAME,
				VM_VENDOR, VM_VERSION }));
		String st = ctxt.format(new Date((Long) al.get(START_TIME)));
		long d = (Long) al.get(UPTIME);
		long days = (int) (d / DAY);
		d %= DAY;
		long hours = (int) (d / HOUR);
		d %= HOUR;
		long mins = (int) (d / MIN);
		d %= MIN;
		long secs = (int) (d / SEC);
		b.put(UPTIME, days + " days, " + hours + " hours, " + mins + " min, " + secs + " sec (since " + st + ")");
		b.put("Virtual Machine", ctxt.getValue(al, VM_NAME) + " (version: " + ctxt.getValue(al, VM_VERSION) + ", vendor: " + ctxt.getValue(al, VM_VENDOR)
				+ ")");
		
		al = ctxt.getAsMap(mbs.getAttributes(ObjectName.getInstance("java.lang:type=OperatingSystem"), new String[] { AVAILABLE_PROCESSORS, NAME,
				VERSION, PROCESS_CPU_TIME, TOTAL_PHYSICAL_MEMORY_SIZE, FREE_PHYSICAL_MEMORY_SIZE, MAX_FILE_DESCRIPTOR_COUNT,
				OPEN_FILE_DESCRIPTOR_COUNT, TOTAL_SWAP_SPACE_SIZE, FREE_SWAP_SPACE_SIZE }));
		
		// OS (nested)
		InfoBean c = new InfoBean("OS Overview");
		c.put("Platform", ctxt.getValue(al, NAME) + " " + ctxt.getValue(al, VERSION));
		c.put("Phys. memory", "total: " + ctxt.getValue(al, TOTAL_PHYSICAL_MEMORY_SIZE) + ", free: " + ctxt.getValue(al, FREE_PHYSICAL_MEMORY_SIZE));
		c.put("Swap memory", "total: " + ctxt.getValue(al, TOTAL_SWAP_SPACE_SIZE) + ", free: " + ctxt.getValue(al, FREE_SWAP_SPACE_SIZE));
		c.put("CPUs", ctxt.getValue(al, AVAILABLE_PROCESSORS));
		beans.add(c);
		
		// Process cont'd
		d = (Long) al.get(PROCESS_CPU_TIME);
		d = d / 1000000;
		days = d / DAY;
		d %= DAY;
		hours = d / HOUR;
		d %= HOUR;
		mins = d / MIN;
		d %= MIN;
		secs = d / SEC;
		b.put("Process CPU time", days + " days, " + hours + " hours, " + mins + " min, " + secs + " sec");
		b.put("File Descriptors", "open: " + ctxt.getValue(al, OPEN_FILE_DESCRIPTOR_COUNT) + ", max: " + ctxt.getValue(al, MAX_FILE_DESCRIPTOR_COUNT));
		
		al = ctxt.getAsMap(
			mbs.getAttributes(
				ObjectName.getInstance("java.lang:type=Threading"), 
				new String[] { THREAD_COUNT, PEAK_THREAD_COUNT,TOTAL_STARTED_THREAD_COUNT }
			)
		);
		b.put(
			"Threading", 
			String.format(
				"current: %,d, peak: %,d, total started: %,d",
				ctxt.getNumber(al, THREAD_COUNT),
				ctxt.getNumber(al, PEAK_THREAD_COUNT),
				ctxt.getNumber(al, TOTAL_STARTED_THREAD_COUNT)
			)
		);
		
		al = ctxt.getAsMap(mbs.getAttributes(ObjectName.getInstance("java.lang:type=ClassLoading"), new String[] { LOADED_CLASS_COUNT,
				TOTAL_LOADED_CLASS_COUNT, UNLOADED_CLASS_COUNT }));
		b.put(
			"Class loading", 
			String.format(
				"loaded: %,d, total: %,d, unloaded: %,d", 
				ctxt.getNumber(al, LOADED_CLASS_COUNT),
				ctxt.getNumber(al, TOTAL_LOADED_CLASS_COUNT),
				ctxt.getNumber(al, UNLOADED_CLASS_COUNT)
			)
		);
		
		beans.add(b);
		
		// 1. Memory:
		CompositeData cd = (CompositeData) mbs.getAttribute(ObjectName.getInstance("java.lang:type=Memory"), "HeapMemoryUsage");
		b = new InfoBean("Memory Overview");
		b.put(
			"Heap Memory Usage", 
			String.format(
				"committed: %,d, , init: %,d, max: %,d, used: %,d",
				cd.get("committed"),
				cd.get("init"), 
				cd.get("max"),
				cd.get("used")
			)
		);
		cd = (CompositeData) mbs.getAttribute(ObjectName.getInstance("java.lang:type=Memory"), "NonHeapMemoryUsage");
		b.put(
			"Non-Heap Memory Usage", 
			String.format(
				"committed: %,d, init: %,d, max: %,d, used: %,d",
				cd.get("committed"),
				cd.get("init"),
				cd.get("max"),
				cd.get("used")
			)
		);
		beans.add(b);
		
		b = new InfoBean("Memory Details");
		// details:
		ons = mbs.queryNames(ObjectName.getInstance("java.lang:type=MemoryPool,name=*"), null);
		for (ObjectName on : ons) {
			cd = (CompositeData) mbs.getAttribute(on, "Usage");
			b.put(
				"Pool \"" + on.getKeyProperty("name") + "\"", 
				String.format(
					"committed: %,d, init: %,d, max: %,d, used: %,d",
					cd.get("committed"),
					cd.get("init"),
					cd.get("max"),
					cd.get("used")
				)
			);
		}
		
		// collectors
		ons = mbs.queryNames(ObjectName.getInstance("java.lang:type=GarbageCollector,*"), null);
		for (ObjectName on : ons) {
			al = ctxt.getAsMap(mbs.getAttributes(on, new String[] { COLLECTION_COUNT, COLLECTION_TIME }));
			b.put(
				"Collector \"" + on.getKeyProperty("name") + "\"", 
				String.format(
					"collections: %,d, total time: %,dms",
					ctxt.getNumber(al, COLLECTION_COUNT),
					ctxt.getNumber(al, COLLECTION_TIME)
				)
			);
		}
		beans.add(b);
		
		// web servers
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.servletjsp.server.ServerResource,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean("Web Server: " + on.getKeyProperty("name"));
			al = ctxt.getAsMap(mbs.getAttributes(on, new String[] { REQUESTS, REQUESTS_ACTIVE, REQUESTS_ACTIVE_MAX, REQUESTS_TIME_AVE,
					REQUESTS_TIME_MAX, RESPONSES1XX, RESPONSES2XX, RESPONSES3XX, RESPONSES4XX, RESPONSES5XX }));
			b.put(
				"Number requests", 
				String.format("%,d",ctxt.getNumber(al, REQUESTS))
			);
			b.put(
				"Active requests", 
				String.format(
					"%,d (max: %,d)",
					ctxt.getNumber(al, REQUESTS_ACTIVE),
					ctxt.getNumber(al, REQUESTS_ACTIVE_MAX)
				)
			);
			b.put(
				"Request duration", 
				String.format(
					"ave: %,dms, max: %,dms",
					ctxt.getNumber(al, REQUESTS_TIME_AVE),
					ctxt.getNumber(al, REQUESTS_TIME_MAX)
				)
			);
			b.put(
				"Response codes", 
				String.format(
					"1xx: %,d, 2xx: %,d, 3xx: %,d, 4xx: %,d, 5xx: %,d",
					ctxt.getNumber(al, RESPONSES1XX),
					ctxt.getNumber(al, RESPONSES2XX),
					ctxt.getNumber(al, RESPONSES3XX),
					ctxt.getNumber(al, RESPONSES4XX),
					ctxt.getNumber(al, RESPONSES5XX)
				)
			);
			beans.add(b);
		}
		
		// web apps overview
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.servletjsp.webapp.WacResource,*"), null);
		if (ons.size() > 0) {
			int maxSessions = 0;
			int sumMaxSessions = 0;
			int sumSessions = 0;
			String maxSessionsComponent = null;
			for (ObjectName on : ons) {
				al = ctxt.getAsMap(mbs.getAttributes(on, new String[] { MAX_SESSIONS, SESSIONS }));
				int sessions = Integer.parseInt(ctxt.getValue(al, SESSIONS));
				if (sessions > maxSessions) {
					maxSessions = sessions;
					maxSessionsComponent = on.getKeyProperty("name");
				}
				sumSessions += sessions;
				sumMaxSessions += Integer.parseInt(ctxt.getValue(al, MAX_SESSIONS));
			}
			b = new InfoBean("Web Apps Overview");
			b.put("Running Web applications", Integer.toString(ons.size()));
			b.put(SESSIONS, sumSessions + " (max: " + sumMaxSessions + ", maxapp:" + maxSessions
					+ (maxSessionsComponent != null ? " by " + maxSessionsComponent : "") + ")");
			beans.add(b);
		}
		
		// timer
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.timer.DispatcherResource,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean("Timer Dispatcher: " + on.getKeyProperty("name"));
			al = ctxt.getAsMap(mbs.getAttributes(on, new String[] { NUM_TIMERS, NUM_ERRORED_TIMERS, NUM_LOCKED_TIMER, NUM_ZOMBIE_TIMERS }));
			b.put("Timers", "current: " + ctxt.getValue(al, NUM_TIMERS) + ", locked: " + ctxt.getValue(al, NUM_LOCKED_TIMER) + ", errored: "
					+ ctxt.getValue(al, NUM_ERRORED_TIMERS) + ", zombies: " + ctxt.getValue(al, NUM_ZOMBIE_TIMERS));
			beans.add(b);
		}
		
		// threadpools
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.work.ThreadPoolImpl,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean("Load and Concurrency: " + on.getKeyProperty("name"));
			al = ctxt.getAsMap(mbs.getAttributes(on, new String[] { MAX_CONCURRENCY_ALLOWED, MAX_CONCURRENCY_REACHED, MAX_QUEUE_LENGTH,
					QUEUE_LENGTH, TASKS_COMPLETED }));
			b.put("Queue", ctxt.getValue(al, QUEUE_LENGTH) + " (max:" + ctxt.getValue(al, MAX_QUEUE_LENGTH) + ")");
			b.put("Max concurrency", ctxt.getValue(al, MAX_CONCURRENCY_REACHED) + " (allowed:" + ctxt.getValue(al, MAX_CONCURRENCY_ALLOWED) + ")");
			b.put("Tasks completed", ctxt.getValue(al, TASKS_COMPLETED));
			beans.add(b);
		}
		// datasources
		ons = mbs.queryNames(ObjectName.getInstance("zfabrik:type=com.zfabrik.impl.db.data.PoolingDataSource,*"), null);
		for (ObjectName on : ons) {
			b = new InfoBean("Data Source: " + on.getKeyProperty("name"));
			al = ctxt.getAsMap(mbs.getAttributes(on,
					new String[] { EXPIRED_CONNECTIONS, MAX_CONCURRENT_CONNECTIONS, MAX_IN_USE_CONNECTIONS, NUM_CONNECTIONS_CREATED,
							NUM_CONNECTIONS_SERVED, NUM_IN_USE_CONNECTIONS, NUM_SPARE_CONNECTIONS, CONNECTION_EXPIRATION }));
			b.put("Current connections", "in use: " + ctxt.getValue(al, NUM_IN_USE_CONNECTIONS) + ", spare: "
					+ ctxt.getValue(al, NUM_SPARE_CONNECTIONS));
			b.put("Counted connections", "served: " + ctxt.getValue(al, NUM_CONNECTIONS_SERVED) + ", created: "
					+ ctxt.getValue(al, NUM_CONNECTIONS_CREATED) + ", expired: " + ctxt.getValue(al, EXPIRED_CONNECTIONS) + " (after "
					+ ctxt.getValue(al, CONNECTION_EXPIRATION) + "ms)");
			b.put("Concurrent connections", "reached: " + ctxt.getValue(al, MAX_CONCURRENT_CONNECTIONS) + ", allowed: "
					+ ctxt.getValue(al, MAX_IN_USE_CONNECTIONS));
			beans.add(b);
		}

	
	}

	@Override
	public void init(IAdminProviderContext context) {
		context.setTabular(false);
		ctxt=context;
	}

}
