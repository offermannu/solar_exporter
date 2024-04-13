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

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class InfoBean {
	private List<InfoBeanAttribute> attributes = new LinkedList<InfoBeanAttribute>();
	private List<InfoBeanAction>  actions = new LinkedList<InfoBeanAction>();
	private String title;
	
	public InfoBean(String title) {
		super();
		this.title = title;
	}
	
	public List<InfoBeanAttribute> getAttributes() {
		return attributes;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	
	public void put(String name, String value) {
		this.attributes.add(new InfoBeanAttribute(name, value));
	}

	public void add(InfoBeanAction action) {
		this.actions.add(action);
	}
	
	public void sortAttributesByKey() {
		Collections.sort(this.attributes, new Comparator<InfoBeanAttribute>() {
			public int compare(InfoBeanAttribute o1, InfoBeanAttribute o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
	}

	public List<InfoBeanAction> getActions() {
		return this.actions;
	}

}
