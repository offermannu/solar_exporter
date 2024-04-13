/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.tests;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zfabrik.impl.workers.MessageExchange;



public class MessageExchangeTests {
	private MessageExchange ax, bx;
	private LineParsingStream aw, bw;

	
	@Before
	public void setup() {
		
		// two pipes

		
		aw = new LineParsingStream();
		ax = new MessageExchange(
			Logger.getLogger("ax"),
			new EchoMessageHandler(),
			new SimpleThreadPool(),
			aw
		);
				
		bw = new LineParsingStream();
		bx = new MessageExchange(
			Logger.getLogger("bx"),
			new EchoMessageHandler(),
			new SimpleThreadPool(),
			bw
		);

		bw.setLineHandler(new ILineHandler() {
			public void processLine(String line) {
				try {
					ax.processLine(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		aw.setLineHandler(new ILineHandler() {
			public void processLine(String line) {
				try {
					bx.processLine(line);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	@After
	public void tearDown() {
		this.ax.close();
		this.bx.close();
	}

	@Test
	public void simpleMessage() throws Exception {
		Map<String,Serializable> m = new HashMap<String, Serializable>();
		m.put("in", "hallo");
		m = ax.sendMessage(m, Long.MAX_VALUE>>1);
		Assert.assertTrue("Expected echo, got "+m.get("out"), "hallo".equals(m.get("out")));
	}

	@Test
	public void largeMessage() throws Exception {
		Map<String,Serializable> m = new HashMap<String, Serializable>();
		StringBuilder sb = createLongString(1024*1024*10);
		m.put("in", sb.toString());
		m = ax.sendMessage(m, Long.MAX_VALUE>>1);
		String r = (String) m.get("out");
		Assert.assertTrue("Expected echo, got "+(r==null? "null":r.length()), sb.toString().equals(r));
	}


	@Test
	public void manyConcurrentMessages() throws Exception {
		final List<String> errors = new LinkedList<String>();
		final List<Integer> count = new ArrayList<Integer>(1);
		count.add(0);
		int size = 100;
		int length= 10;
		sendManyMessages(errors, count, size, length);
		synchronized (errors) {
			Assert.assertTrue("Got "+errors, errors.isEmpty());
		}
		synchronized (count) {
			Assert.assertTrue("Completed "+count.get(0), count.get(0)==size);
		}
	}
	
	@Test
	public void manyLargeConcurrentMessages() throws Exception {
		final List<String> errors = new LinkedList<String>();
		final List<Integer> count = new ArrayList<Integer>(1);
		count.add(0);
		int size = 100;
		int length= 2*65536;
		sendManyMessages(errors, count, size, length);
		synchronized (errors) {
			Assert.assertTrue("Got "+errors, errors.isEmpty());
		}
		synchronized (count) {
			Assert.assertTrue("Completed "+count.get(0), count.get(0)==size);
		}
	}

	private void sendManyMessages(final List<String> errors,final List<Integer> count, final int size, final int length) {
		List<Runnable> tasks = new ArrayList<Runnable>(size);
		for (int i=0; i<size; i++) {
			final int j = i;
			tasks.add(new Runnable() {
				public void run() {
					Map<String,Serializable> m = new HashMap<String, Serializable>();
					String base = "hallo "+j;
					String load = createLongString(length).replace(0,base.length(), base).toString();
					m.put("in", load);
					try {
						m = ax.sendMessage(m, Long.MAX_VALUE>>1);
						String r = (String) m.get("out");
						if (!load.equals(r)) {
							synchronized (errors) {
								errors.add("task "+j+" got "+(r==null? "null" : r.length()));
							}
						}
					} catch (Exception e) {
						synchronized (errors) {
							errors.add("task "+j+" got "+e.toString());
						}
					}
					synchronized (count) {
						count.set(0, count.get(0)+1);
					}
				}
			});
		}
		new SimpleThreadPool().execute(true, tasks);
	}

	private StringBuilder createLongString(int size) {
		StringBuilder sb = new StringBuilder(size);
		for (int i=0; i<size; i++) {
			sb.append((char)('a'+(i%26)));
		}
		return sb;
	}


}
