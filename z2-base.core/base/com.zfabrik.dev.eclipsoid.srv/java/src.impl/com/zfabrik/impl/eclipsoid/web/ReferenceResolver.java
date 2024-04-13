/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.web;

import static com.zfabrik.util.expression.X.eq;
import static com.zfabrik.util.expression.X.val;
import static com.zfabrik.util.expression.X.var;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.zfabrik.components.Components;
import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;
import com.zfabrik.util.StringUtils;

public class ReferenceResolver extends HttpServlet {
	private static final long serialVersionUID = 1556165353726413635L;
	private final static Logger LOG = Logger.getLogger(ReferenceResolver.class.getName());

	private List<String> projectNames;
	private PrintWriter w_out;

	public void setProjectName(String projectName) {
		this.projectNames = StringUtils.splitString(projectName);
		if (this.projectNames == null) {
			this.projectNames = Collections.emptyList();
		}
	}
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		setProjectName(request.getParameter("projectName"));
		
		try  {
			response.setContentType("text/plain");
			response.setCharacterEncoding("UTF-8");
			this.w_out = response.getWriter();

			for (String project : projectNames) {
				generateReferencesFor(project);	
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE,"Failed to resolve references of " + this.projectNames, e);
			e.printStackTrace();
			
			try {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
						"Failed to resolve references of " + this.projectNames);
			} catch (IOException e1) {}
		}
		
	}

	private void generateReferencesFor(String projectName) throws IOException {
		Set<String> refs = new HashSet<String>();
		
		Collection<String> components;
		if (projectName.lastIndexOf('/')<0) {
			// If it has no "/", it must be a module name.
			// In that case, we look for all Java components of the module
			components = Components.findComponents(eq(var(IComponentDescriptor.COMPONENT_TYPE),val(IJavaComponent.TYPE)))
						 .stream().filter(c->projectName.equals(moduleOf(c))).collect(Collectors.toList());
		} else {
			// just the component
			components = Collections.singleton(projectName);
		}
		
		for (String jc : components) {
			Properties p = Components.getComponentProperties(jc);
			if (p != null) {
	            addRefs(refs,p.getProperty(IJavaComponent.PUBREFS),false);
	            addRefs(refs,p.getProperty(IJavaComponent.PUBINCS),true);
	            addRefs(refs,p.getProperty(IJavaComponent.PRIREFS),false);
	            addRefs(refs,p.getProperty(IJavaComponent.PRIINCS),true);
	            addRefs(refs,p.getProperty(IJavaComponent.TESTREFS),false);
	            addRefs(refs,p.getProperty(IJavaComponent.TESTINCS),true);
				
				// add z-default dependency 
				refs.add("com.zfabrik.core.api/java");
			}
		}		
		w_out.print(projectName + " = ");
		w_out.print(refs.toString());
		w_out.println();
	}
	
	/**
	 * Extracts module name from component name (i.e. the "/" separated prefix)
	 */
	private static String moduleOf(String componentName) {
		int p = componentName.lastIndexOf('/');
		if (p>=0) {
			return componentName.substring(0,p);
		}
		return null;
	}

	private void addRefs(Set<String> refs, String rp, boolean incl) {
		Collection<String> n = JavaComponentUtil.parseDependencies(rp);
		
		if (!incl) {
			// replay transitivity of refs
			for (String r : n) {
				if (refs.add(r)) {
					IComponentDescriptor desc = IComponentsLookup.INSTANCE.lookup(r,IComponentDescriptor.class);
					if (desc != null) {
						addRefs(refs,desc.getProperties().getProperty(IJavaComponent.PUBREFS),false);
					}
				}
			}
		} else {
			refs.addAll(n);
		}
	}

}
