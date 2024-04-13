/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.dev;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IDependencyComponent;
import com.zfabrik.resources.ResourceBusyException;
import com.zfabrik.resources.provider.Resource;
import com.zfabrik.util.runtime.Foundation;

/**
 * Core revision checking resource implementing the component type
 * <code>com.zfabrik.coreRevisionCheck</code>.
 * <p>
 * The purpose of this component type is
 * to enable a core revision requirement checking before attaining a particular
 * system state. Currently the core build no (see {@link Foundation#getCoreBuildVersion()} is used is core revision. Using this component 
 * type an expectation of a certain core build no can be tested.
 * <p>
 * When the present core revision is found to be too old, the revision checker
 * may respond in two ways: Either
 * <ul>
 * <li>Terminate the local VM (default)</li>
 * <li>Throw an exception that effectively makes sure a given system state cannot 
 * be reached</li>
 * </ul>
 * 
 * <p>
 * Supported properties:
 * <dl>
 * <dt>coreRevision.min</dt>
 * <dd>The minimum core revision (build no. actually) expected</dd>
 * <dt>coreRevision.failAction</dt>
 * <dd>Specifies how to respond to a missed requirement. Either <code>terminate</code> (default) or <code>error</code>. The former
 * leads to a termination of the local VM, the latter to an exception thrown by the checker.
 * </dd>
 * </dl>
 *  
 * <p>
 * A typical revision checker is declared like this:
 * <pre>
 * com.zfabrik.component.type=com.zfabrik.coreRevisionCheck
 * #
 * # current version
 * #
 * coreRevision.min=201012122022
 * #
 * # We throw an error
 * #
 * coreRevision.failAction=error
 * #
 * # make sure every process is checked
 * #
 * com.zfabrik.systemStates.participation=com.zfabrik.boot.main/process_up
 * </pre>
 *  
 * @author hb
 *
 */
public class CoreRevisionCheckResource extends Resource implements IDependencyComponent {
	private static final String CORE_REVISION_FAIL_ACTION = "coreRevision.failAction";
	private static final String CORE_REVISION_MIN = "coreRevision.min";
	private String name;
	private boolean checkedOk;
	
	public CoreRevisionCheckResource(String name) {
		this.name = name;
	}
	
	public <T> T as(Class<T> clz) {
		if (IDependencyComponent.class.equals(clz))  {
			return clz.cast(this);
		}
		return super.as(clz);
	}
	
	public synchronized void invalidate() throws ResourceBusyException {
		this.checkedOk = false;
	}
	
	public synchronized void prepare() {
		if (!checkedOk) {
			IComponentDescriptor d = handle().as(IComponentDescriptor.class);
			String lm = d.getProperty(CORE_REVISION_MIN);
			if (lm==null) {
				throw new IllegalArgumentException("Required component property "+CORE_REVISION_MIN+" missing: "+this.name);
			}
			long r = Long.parseLong(lm.trim());
			if (r>Foundation.getCoreBuildVersion()) {
				String msg = 
					"\n" +
					"*********************************************************************************\n" +
					"NOTE:\n" +
					"The core build number "+Foundation.getCoreBuildVersion()+ " indicates an outdated z2 core distribution!\n\n"+
					"A minimum of "+lm+" was expected in revision check \""+this.name+"\".\n\n" +
					"Please update your core distribution! If you installed via Subversion\n" +
					"checkout, this may be as simple as issueing a 'svn up' command in your\n" +
					"z2 core installation folder.\n"+
					"*********************************************************************************\n";
				String ac = d.getProperty(CORE_REVISION_FAIL_ACTION);
				if (ac!=null) {
					ac = ac.trim();
					if ("error".equals(ac)) {
						throw new IllegalStateException(msg);
					}
				}
				System.err.println(msg);
				System.err.println("The process will be terminated.\n");
				System.err.flush();
				System.exit(1);
			}
			checkedOk=true;
		}
	}
	

}
