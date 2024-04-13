/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.test.db.data;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.dev.z2jupiter.Z2JupiterTestable;
import com.zfabrik.impl.db.data.DataSourceWorkResource;
import com.zfabrik.impl.db.data.Module;
import com.zfabrik.tx.TransactionUtil;
import com.zfabrik.tx.UserTransaction;
import com.zfabrik.work.WorkUnit;

/**
 * Tests for refs #2077
 */
@Z2JupiterTestable(componentName = Module.NAME)
public class ConnectionEnlistingTests {
	
	/**
	 * Test for data source that enlists in work unit
	 */
	@Test
	public void connectionEnlistedInWorkUnit() throws Exception {
		WorkUnit detached = WorkUnit.detach();
		try {
			DataSource ds = IComponentsLookup.INSTANCE.lookup(Module.NAME+"/testDataSourceWorkUnit", DataSource.class);
			Connection conn = WorkUnit.work(()->{
				try (Connection c = ds.getConnection()) {
					Assertions.assertNotNull(c);
					// should return the same connection again
					try (Connection d = ds.getConnection()) {
						Assertions.assertTrue(c == d);
					}
					return c;
				}
			});
			// should be different outside (but the pool may return the same - so we need to retrieve two connections)
			try (Connection e = ds.getConnection()) {
				Assertions.assertNotNull(e);
				try (Connection f = ds.getConnection()) {
					Assertions.assertTrue(conn != e || conn != f);
					Assertions.assertTrue(f!=e);
				}
			}
		} finally {
			WorkUnit.attach(detached);
		}
	}

	/**
	 * Test for data source that enlists in jta
	 */
	@Test
	public void connectionEnlistedInJta() throws Exception {
		// note: we already have an outer workunit. 
		String dataSourceName = Module.NAME+"/testDataSourceJta";
		DataSource ds = IComponentsLookup.INSTANCE.lookup(dataSourceName, DataSource.class);
		// should be different outside (but the pool may return the same - so we need to retrieve two connections)
		try (Connection e = ds.getConnection()) {
			Assertions.assertNotNull(e);
			try (Connection f = ds.getConnection()) {
				Assertions.assertTrue(f!=e);
			}
		}

		Connection conn = TransactionUtil.run(UserTransaction.get(),()->{
			try (Connection c = ds.getConnection()) {
				Assertions.assertNotNull(c);
				// should return the same connection again
				try (Connection d = ds.getConnection()) {
					Assertions.assertTrue(c == d);
				}
				return c;
			}
		});
		
		// no connection should be bound
		Assertions.assertNull(WorkUnit.getCurrent().getResource(DataSourceWorkResource.computeWorkUnitResourceKey(dataSourceName)));
		
		// next tx should have different connections
		TransactionUtil.run(UserTransaction.get(),()->{
			try (Connection c = ds.getConnection()) {
				// detach tx
				WorkUnit detach = WorkUnit.detach();
				try {
					TransactionUtil.run(UserTransaction.get(),()->{
						try (Connection d = ds.getConnection()) {
							// should be really different
							Assertions.assertTrue(c != d);
						}
					});
				} finally {
					WorkUnit.attach(detach);
				}
			}
		});

		// outside should be different again (even though we have a workunit)
		try (Connection e = ds.getConnection()) {
			Assertions.assertNotNull(e);
			try (Connection f = ds.getConnection()) {
				Assertions.assertTrue(conn != e || conn != f);
				Assertions.assertTrue(f!=e);
			}
		}
		
	}

	/**
	 * Test that auto commit is reset
	 */
	@SuppressWarnings("resource")
	@Test
	public void connectionAutoCommitReset() throws Exception {
		// note: we already have an outer workunit. 
		DataSource ds = IComponentsLookup.INSTANCE.lookup(Module.NAME+"/testDataSourceWorkUnit", DataSource.class);
		Connection f;
		try (Connection e = ds.getConnection()) {
			f = e;
			e.setAutoCommit(true);
		}
		// auto commit should have been reset after close
		try (Connection g = ds.getConnection()) {
			Assertions.assertTrue(g==f);
			Assertions.assertFalse(g.getAutoCommit());
		}
	}

}
