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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import com.zfabrik.work.IThreadPool;

final class SimpleThreadPool implements IThreadPool {
	public void setMaxConcurrency(int maxconcurrency) {	}

	public boolean isPoolThread(Thread t) {	return false; }

	public int getMaxConcurrency() {return 1000;}

	public int getMaxAchievedConcurrency() {return 0;}

	public <T> T executeAs(final Callable<T> callable, boolean maxconcurrency) throws Exception {
		return callable.call();
	}

	@Override
	public void executeAs(final Runnable runnable, final boolean maxconcurrency) {
		try {
			this.executeAs(new Callable<Void>() {
					public Void call() throws Exception {
						runnable.run();
						return null;
					}
				},
				maxconcurrency
			);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void execute(boolean block, Runnable... runnables) {
		execute(block, Arrays.asList(runnables));
	}

	@Override
	public void execute(boolean block, Collection<? extends Runnable> runnables) {
		final List<Integer> countDown = new ArrayList<Integer>(1);
		countDown.add(runnables.size());
		synchronized (countDown) {
			for (final Runnable r : runnables) {
				new Thread() {
					public void run() {
						r.run();
						synchronized (countDown) {
							countDown.set(0,countDown.get(0)-1);
							countDown.notify();
						}
					};
				}.start();
			}
			if (block) {
				while (countDown.get(0)>0) {
					try {countDown.wait();} catch (InterruptedException e) { throw new RuntimeException(e); }
				}
			}
		}
	}
}