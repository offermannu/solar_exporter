/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.mvncr;

import static com.zfabrik.impl.mvncr.MvnFragmentResource.MVNCR_COMPONENT;
import static com.zfabrik.impl.mvncr.MvnRepositoryImpl.MVN_EXCLUDED;
import static com.zfabrik.impl.mvncr.MvnRepositoryImpl.MVN_MANAGED;
import static com.zfabrik.impl.mvncr.MvnRepositoryImpl.MVN_ROOTS;
import static com.zfabrik.util.expression.X.val;
import static com.zfabrik.util.expression.X.var;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.resources.IResourceHandle;

/**
 * Helper class to merge all mvncr configuration along 
 * repository and fragments.
 * 
 * 
 * @author hb
 *
 */
public class MvnConfigCollector {

	
	private Set<String> traversed = new HashSet<String>();
	private Set<String> roots = new HashSet<String>();
	private Set<String> managed = new HashSet<String>();
	private Set<String> excluded = new HashSet<String>();
	
	private IResourceHandle handle;
	
	public MvnConfigCollector(IResourceHandle handle) {
		this.handle = handle;
	}
	
	/**
	 * Add all from a repo or fragment component - incl. recursion
	 */
	public boolean add(String componentName) {
		if (!traversed.contains(componentName)) {
			traversed.add(componentName);
			// NOTE: This lookup is resolving links!
			IComponentDescriptor d = IComponentsLookup.INSTANCE.lookup(componentName, IComponentDescriptor.class);
			if (d!=null) {
				traversed.add(d.getName());
				// check for type
				if (MvnRepositoryResource.TYPE.equals(d.getType()) || MvnFragmentResource.TYPE.equals(d.getType())) {
					// ok
					Properties p = d.getProperties();
					splitAndAdd(p.getProperty(MVN_ROOTS),this.roots);
					splitAndAdd(p.getProperty(MVN_EXCLUDED),this.excluded);
					splitAndAdd(p.getProperty(MVN_MANAGED),this.managed);

					try {
						String repository = d.getProperties().getProperty(MvnRepositoryImpl.MVN_REPOSITORY, componentName).trim();
						
						// next look for fragments
						for (String cn : IComponentsManager.INSTANCE.findComponents(
								val(repository).in(var(MVNCR_COMPONENT)).or(val(repository).in(var(MvnRepositoryImpl.MVN_REPOSITORY))
						))) {
							if (add(cn)) {
								this.handle.addDependency(IComponentsLookup.INSTANCE.lookup(cn, IResourceHandle.class));
							}
						}
					} catch (IOException e) {
						throw new RuntimeException("Failed to look for fragments of MVNCR (or fragment) "+componentName);
					}
					return true;
				} else {
					LOG.warning("Component \""+d.getName()+"\" is neither of type "+MvnRepositoryResource.TYPE+" nor of type "+MvnFragmentResource.TYPE+". Ignoring");
				}
			}
		}
		return false;
	}
	
	private void splitAndAdd(String list, Set<String> collect) {
		if (list!=null) {
			StringTokenizer tk = new StringTokenizer(list,",");
			while (tk.hasMoreTokens()) {
				String e = tk.nextToken().trim();
				if (e.length()>0) {
					collect.add(e);
				}
			}
		}
	}

	public Set<String> getExcluded() {
		return excluded;
	}
	public Set<String> getManaged() {
		return managed;
	}
	public Set<String> getRoots() {
		return roots;
	}

	private final static Logger LOG = Logger.getLogger(MvnConfigCollector.class.getName());
}
