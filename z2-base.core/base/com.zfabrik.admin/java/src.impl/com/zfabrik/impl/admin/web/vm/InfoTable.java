/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.admin.web.vm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class InfoTable {
	private List<String> columns;
	private List<InfoTableRow> rows = new LinkedList<InfoTableRow>();

	public InfoTable(List<String> columns) {
		super();
		this.columns = columns;
	}
	
	public void addRow(Map<String,String> row) {
		this.rows.add(new InfoTableRow(row, null));
	}

	public void addRow(InfoTableRow row) {
		this.rows.add(row);
	}
	
	public List<InfoTableRow> getRows() {
		return rows;
	}
	
	public List<String> getColumns() {
		return columns;
	}
	
}
