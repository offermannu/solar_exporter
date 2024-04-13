/*
 * z2-Environment
 * 
 * Copyright(c) ZFabrik Software GmbH & Co. KG
 * 
 * contact@zfabrik.de
 * 
 * http://www.z2-environment.eu
 */
package com.zfabrik.impl.eclipsoid.z2info;

import static com.zfabrik.util.expression.X.eq;
import static com.zfabrik.util.expression.X.val;
import static com.zfabrik.util.expression.X.var;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.FloydWarshallShortestPaths;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.java.IJavaComponent;
import com.zfabrik.components.java.JavaComponentUtil;

public class DependenciesRetriever extends AbstractRetriever {

	private static final String DEPENDENCIES = "dependencies";

	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || DEPENDENCIES.equalsIgnoreCase(type);
	}
	
	public static String module(String cn) {
		int p = cn.lastIndexOf('/');
		return p<0? cn : cn.substring(0,p);
	}

	public static String component(String cn) {
		int p = cn.lastIndexOf('/');
		return p<0? cn : cn.substring(p+1);
	}

	public void provideInfoInto(JSONObject result) throws IOException {
		
		JSONObject dependencyJSON = new JSONObject();
		result.put("dependencies", dependencyJSON);

		SimpleDirectedGraph<String, Z2Dependency> dependencyGraph = new SimpleDirectedGraph<String, Z2Dependency>(Z2Dependency.class);

		Collection<String> javaComps = IComponentsManager.INSTANCE.findComponents(eq(var("com.zfabrik.component.type"), val("com.zfabrik.java") ));

		// add nodes
		for (String javaComp : javaComps) {

			// assume component is always <project-name>/<component-name>
			String projectName = module(javaComp);

			dependencyGraph.addVertex(projectName + "#api");
			dependencyGraph.addVertex(projectName + "#impl");
		}

		// add edges
		for (String javaComp : javaComps) {

			IComponentDescriptor cDesc = IComponentsManager.INSTANCE.getComponent(JavaComponentUtil.fixJavaComponentName(javaComp));

			// assume component is always <project-name>/<component-name>
			String projectName = module(javaComp);

			// add public dependencies (public dependencies are available in api and impl)
			HashSet<String> adjacent = new HashSet<String>();
			adjacent.addAll(getShallowRefs(cDesc, IJavaComponent.PUBREFS,false)); 
			adjacent.addAll(getShallowRefs(cDesc, IJavaComponent.PUBINCS,true));

			for (String adjNode : adjacent) {

				String mod = module(adjNode);
				String adjName = mod + "#api";

				if (dependencyGraph.containsVertex(adjName)) {

					if (! mod.equals(projectName) && ! dependencyGraph.containsEdge(projectName + "#api", adjName)) {
						try {
							dependencyGraph.addEdge(projectName + "#api", adjName);
						} catch (IllegalArgumentException e) {
							throw new IllegalArgumentException("Failed to add edge " + projectName + "#api -> " + adjName);
						}
					}

					if (! dependencyGraph.containsEdge(projectName + "#impl", adjName)) {
						try {
							dependencyGraph.addEdge(projectName + "#impl", adjName);
						} catch (IllegalArgumentException e) {
							throw new IllegalArgumentException("Failed to add edge " + projectName + "#impl -> " + adjName);
						}
					}

				} else {
					// it could be a files component
					IComponentDescriptor c = IComponentsManager.INSTANCE.getComponent(mod + "/java");
					if (c != null) {
						dependencyGraph.addVertex(adjName);
					} else {
						addIllegalDependencyToJSON(dependencyJSON, projectName, mod);
					}
				} 
			}

			// add private dependencies (private dependencies are available in impl only)
			adjacent.clear();

			adjacent.addAll(getShallowRefs(cDesc, IJavaComponent.PRIREFS,false)); 
			adjacent.addAll(getShallowRefs(cDesc, IJavaComponent.PRIINCS,true));

			for (String adjNode : adjacent) {

				String adjComp = module(adjNode);
				String adjName = adjComp + "#api";

				if (! dependencyGraph.containsVertex(adjName)) {
					// it could be a files component
					IComponentDescriptor c = IComponentsManager.INSTANCE.getComponent(adjComp + "/java");
					if (c != null) {

						dependencyGraph.addVertex(adjName);

					} else {
						addIllegalDependencyToJSON(dependencyJSON, projectName, adjComp);
					}
				}

				if (dependencyGraph.containsVertex(adjName)) {

					if (! dependencyGraph.containsEdge(projectName + "#impl", adjName)) {
						dependencyGraph.addEdge(projectName + "#impl", adjName);
					}

				} else {

					addIllegalDependencyToJSON(dependencyJSON, projectName, adjComp);
				} 
			}

		}

		// generate JSON response

		CycleDetector<String, Z2Dependency> cycleDetector = new CycleDetector<String, Z2Dependency>(dependencyGraph);

		if (cycleDetector.detectCycles()) {
			dependencyJSON.put("Dependency Graph contains cycles!", true);

		} else {
			FloydWarshallShortestPaths<String, Z2Dependency> dependencyPaths = new FloydWarshallShortestPaths<String, Z2Dependency>(dependencyGraph);

			JSONObject projectsJSON = new JSONObject();
			dependencyJSON.put("transitive dependencies closure", projectsJSON);

			for (String javaComp : javaComps) {

				// assume component is always <project-name>/<component-name>
				String projectName = module(javaComp);
				String apiName = projectName + "#api";
				String implName = projectName + "#impl";

				JSONObject javaCompJSON;
				String projectsNodeName = projectName + "?type=project";
				if (projectsJSON.has(projectsNodeName)) {
					javaCompJSON = projectsJSON.getJSONObject(projectsNodeName);
				} else {
					javaCompJSON = new JSONObject();
					projectsJSON.put(projectsNodeName, javaCompJSON);
				}

				// dependencies of api 
				if (dependencyGraph.containsVertex(apiName)) {
					
					List<GraphPath<String, Z2Dependency>> apiPaths = dependencyPaths.getShortestPaths(apiName);
					for (GraphPath<String, Z2Dependency> p : apiPaths) {
						
						String compsNodeName = getModuleName(p.getEndVertex()) + "?type=project";
						if (! javaCompJSON.has(compsNodeName)) {
							
							JSONObject dependendNode = new JSONObject();
							dependendNode.put("api", pathToString(p));
							javaCompJSON.put(compsNodeName, dependendNode);
						}
					}
				}

				if (dependencyGraph.containsVertex(implName)) {
					
					List<GraphPath<String, Z2Dependency>> implPaths = dependencyPaths.getShortestPaths(implName);
					for (GraphPath<String, Z2Dependency> p : implPaths) {
						
						String compsNodeName = getModuleName(p.getEndVertex()) + "?type=project";
						if (! javaCompJSON.has(compsNodeName)) {
							
							JSONObject dependendNode = new JSONObject();
							dependendNode.put("impl", pathToString(p));
							javaCompJSON.put(compsNodeName, dependendNode);
						}
					}
				}
			}
		}
	}

	private void addIllegalDependencyToJSON(JSONObject dependencyJSON, String projectName, String adjName) {
		JSONObject illegalJSON;
		if (dependencyJSON.has("illegal dependencies")) {
			illegalJSON = dependencyJSON.getJSONObject("illegal dependencies");
		} else {
			illegalJSON = new JSONObject();
			dependencyJSON.put("illegal dependencies", illegalJSON);
		}

		JSONObject javaCompJSON;
		String projectsNodeName = projectName + "?type=project";
		if (illegalJSON.has(projectsNodeName)) {
			javaCompJSON = illegalJSON.getJSONObject(projectsNodeName);
		} else {
			javaCompJSON = new JSONObject();
			illegalJSON.put(projectsNodeName, javaCompJSON);
		}

		javaCompJSON.put(adjName, new JSONObject());
	}

	/*
	 * retrieve the direct references for the given reference type (private, public, ...) and for the given component descriptor
	 */
	private Collection<String> getShallowRefs(IComponentDescriptor cDesc, String kind, boolean incl) {
		return JavaComponentUtil.parseDependencies(cDesc.getProperties().getProperty(kind));
	}
	
	private String getModuleName(String componentName) {
		String result = null;
		if (componentName != null) {
			result = componentName.split("#")[0];
		}
		return result;
	}

	private String pathToString(GraphPath<String, Z2Dependency> path) {
		StringBuffer result = new StringBuffer();
		Iterator<Z2Dependency> loopPath = path.getEdgeList().iterator();
		
		if (loopPath.hasNext()) {
			Z2Dependency edge = loopPath.next();
			result.append(getModuleName(edge.getSource().toString())).append(" -> ").append(getModuleName(edge.getTarget().toString()));
			while (loopPath.hasNext()) {
				result.append(" -> ").append(getModuleName(loopPath.next().getTarget().toString()));
			}
		}
		return result.toString();
	}

	/*
	 * DefaultEdge's methods are protected - make them visible
	 */
	@SuppressWarnings("serial")
	public static class Z2Dependency extends DefaultEdge {
		public Object getSource() {
			return super.getSource();
		}
		public Object getTarget() {
			return super.getTarget();
		}
	}
	
	//    private JSONObject getAllRefs(IComponentDescriptor cDesc, int maxDepth) {
	//        if (cDesc == null || --maxDepth == 0) return null;
	//
	//        JSONObject result = new JSONObject();
	//        JSONObject privRefs = getRefs(cDesc, IJavaComponent.PRIREFS, maxDepth);
	//        if (privRefs != null) {
	//            result.put("private?type=ref", privRefs);
	//        }
	//
	//        JSONObject publRefs = getRefs(cDesc, IJavaComponent.PUBREFS, maxDepth);
	//        if (publRefs != null) {
	//            result.put("public?type=ref", publRefs);
	//        }
	//
	//        //		JSONObject testRefs = getRefs(cDesc, IJavaComponent.TESTREFS, maxDepth);
	//        JSONObject testRefs = getRefs(cDesc, "java.testReferences", maxDepth);
	//        if (testRefs != null) {
	//            result.put("test?type=ref", testRefs);
	//        }
	//
	//        return result;
	//    }
	//
	//    private JSONObject getRefs(IComponentDescriptor cDesc, String kind, int maxDepth) {
	//        JSONObject result = new JSONObject();
	//
	//        String refs = cDesc.getProperties().getProperty(kind);
	//        if (refs != null) {
	//            StringTokenizer loopRefs = new StringTokenizer(refs, ",:; \t\n\r\f");
	//            while(loopRefs.hasMoreTokens()) {
	//                String dependComp  = JavaComponentUtil.fixJavaComponentName(loopRefs.nextToken());
	//                IComponentDescriptor cDependDesc = IComponentsManager.INSTANCE.getComponent(dependComp);
	//
	//                String[] projComp = dependComp.split("/");
	//                String projName = projComp[0];
	//
	//                JSONObject dependsNode = getAllRefs(cDependDesc, maxDepth);
	//                result.put(projName + "?type=project", dependsNode);
	//            }
	//        }
	//
	//        if (result.length() > 0) {
	//            return result;
	//        } else {
	//            return null;
	//        }
	//    }
}
