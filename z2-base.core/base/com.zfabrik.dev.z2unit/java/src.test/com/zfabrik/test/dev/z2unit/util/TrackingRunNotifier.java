/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.dev.z2unit.util;

import java.util.LinkedList;
import java.util.List;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.notification.StoppedByUserException;

/**
 * Simple tracking notifier to see what happened
 */
public class TrackingRunNotifier extends RunNotifier {
	
	public static enum EventType { 
		fireTestRunStarted,fireTestRunFinished,fireTestStarted,fireTestFailure,fireTestAssumptionFailed,fireTestIgnored,fireTestFinished
	}
	
	public static class Event {
		EventType type;
		Description description;
		Result result;
		Failure failure;
		
		public EventType getType() {
			return type;
		}
		public Description getDescription() {
			return description;
		}
		public Result getResult() {
			return result;
		}
		public Failure getFailure() {
			return failure;
		}
		public Event(EventType type, Description description) {
			super();
			this.type = type;
			this.description = description;
		}
		public Event(EventType type, Result result) {
			super();
			this.type = type;
			this.result = result;
		}
		public Event(EventType type, Failure failure) {
			super();
			this.type = type;
			this.failure = failure;
		}
		@Override
		public String toString() {
			return "Event [type=" + type + ", description=" + description + ", result=" + result + ", failure=" + failure + "]";
		}
		
	}
	
	private List<Event> events = new LinkedList<>();

	public List<Event> getEvents() {
		return events;
	}
	
	@Override
	public void fireTestRunStarted(Description description) {
		events.add(new Event(EventType.fireTestRunStarted,description));
		super.fireTestRunStarted(description);
	}

	@Override
	public void fireTestRunFinished(Result result) {
		events.add(new Event(EventType.fireTestRunFinished,result));
		super.fireTestRunFinished(result);
	}

	@Override
	public void fireTestStarted(Description description) throws StoppedByUserException {
		events.add(new Event(EventType.fireTestStarted,description));
		super.fireTestStarted(description);
	}

	@Override
	public void fireTestFailure(Failure failure) {
		events.add(new Event(EventType.fireTestFailure,failure));
		super.fireTestFailure(failure);
	}

	@Override
	public void fireTestAssumptionFailed(Failure failure) {
		events.add(new Event(EventType.fireTestAssumptionFailed,failure));
		super.fireTestAssumptionFailed(failure);
	}

	@Override
	public void fireTestIgnored(Description description) {
		events.add(new Event(EventType.fireTestIgnored,description));
		super.fireTestIgnored(description);
	}

	@Override
	public void fireTestFinished(Description description) {
		events.add(new Event(EventType.fireTestFinished,description));
		super.fireTestFinished(description);
	}

}
