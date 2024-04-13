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

import static com.zfabrik.impl.eclipsoid.z2info.DependenciesRetriever.module;
import static com.zfabrik.util.expression.X.val;
import static com.zfabrik.util.expression.X.var;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.zfabrik.components.IComponentDescriptor;
import com.zfabrik.components.IComponentsLookup;
import com.zfabrik.components.IComponentsManager;
import com.zfabrik.components.provider.IComponentsRepository;
import com.zfabrik.svnaccess.ISvnRepository;
import com.zfabrik.util.expression.X;

public class RepositoriesRetriever extends AbstractRetriever {

	private final static Logger logger = Logger.getLogger(RepositoriesRetriever.class.getName());

	private static final String REPOSITORIES = "repositories";
	private static final String DEV_REPO_COMPONENT = "com.zfabrik.dev.repo/devRepo";

	private final static String[] CR_TYPES = {
		"com.zfabrik.svncr", 
		"com.zfabrik.gitcr",
        "com.zfabrik.hubcr",
        "com.zfabrik.mvncr",
		DEV_REPO_COMPONENT
	};

	@Override
	public boolean isProviderFor(String type) {
		return super.isProviderFor(type) || REPOSITORIES.equalsIgnoreCase(type);
	}

	@Override
	public void provideInfoInto(JSONObject result) throws IOException {

		JSONObject reposNode = new JSONObject();
		result.put("repositories", reposNode);

		for (String crType : CR_TYPES) {

			Collection<String> allRepoComps;

			if (DEV_REPO_COMPONENT.equals(crType)) {
				allRepoComps = Collections.singleton(DEV_REPO_COMPONENT);
			} else {
				allRepoComps = IComponentsManager.INSTANCE.findComponents(X.eq(var("com.zfabrik.component.type"), val(crType)));
			}

			for (String crId : allRepoComps) {

				IComponentsRepository cr = IComponentsLookup.INSTANCE.query(crId, IComponentsRepository.class);
				if (cr != null) {

					JSONObject repoCompNode = new JSONObject();

					String crSpecificProps = "";
					if ("com.zfabrik.svncr".equals(crType)) {
						try {
							ISvnRepository svnRepo = IComponentsLookup.INSTANCE.lookup(crId, ISvnRepository.class);
							if (svnRepo != null) {
								String repoUuid = svnRepo.getRepositoryUuid();
								crSpecificProps = "&svnUuid=" + URLEncoder.encode(repoUuid, "UTF-8");
								
								JSONObject svnDetailsNode = new JSONObject();
								repoCompNode.put("SVN details?type=folder", svnDetailsNode);

								svnDetailsNode.put("svn uuid?type=property", repoUuid);
								svnDetailsNode.put("svn root url?type=property", svnRepo.getSvnRootUrl().toString());
								svnDetailsNode.put("svncr path?type=property", svnRepo.getCRPath());
								svnDetailsNode.put("svncr revision?type=property", svnRepo.getCurrentCRRevision());
								
							}
						} catch (IOException e) {
							logger.log(Level.WARNING, "Failed to retrieve UUID for SVNCR '" + crId + "'!", e);
						}
					}

					String repositoryRoot = URLEncoder.encode(String.valueOf(cr.toString()), "UTF-8");
					reposNode.put(crId + "?type=componentRepository&repositoryType=" + crType + "&repositoryURI=" + repositoryRoot + crSpecificProps, repoCompNode);

					//					JSONObject propsNode = new JSONObject();
					//					repoCompNode.put("settings?type=folder", propsNode);

					IComponentDescriptor repoDescr = IComponentsManager.INSTANCE.getComponent(crId);
					for (Entry<Object, Object> repoProp : repoDescr.getProperties().entrySet()) {

						String key = (String) repoProp.getKey();
						String value = (String) (!isSensitiveKey(repoProp.getKey().toString())? repoProp.getValue() : "*****");
						repoCompNode.put(key + "?type=property", value);
					}

					JSONObject compsNode = new JSONObject();
					repoCompNode.put("z2-Projects?type=folder", compsNode);

					Collection<String> foundComponents = cr.findComponents(X.val(true), true);
					if (foundComponents != null) {
						for (String component : foundComponents) {

							// is this component available in the current repository?
							if (cr.getComponent(component) == null) continue;

							String projectName = module(component); // component: <project-name>/<component-name> - cut off the components part
							if (! compsNode.has(projectName)) {
								compsNode.put(projectName + "?type=project", new JSONObject());
							}
						}
					}
				}
			}
		}
	}
}