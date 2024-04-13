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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuilder;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositoryCache;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.AuthenticationSelector;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.MirrorSelector;
import org.eclipse.aether.repository.ProxySelector;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.classpath.ClasspathTransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.ConservativeAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultAuthenticationSelector;
import org.eclipse.aether.util.repository.DefaultMirrorSelector;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

import com.zfabrik.impl.mvncr.util.LoggingRepositoryListener;
import com.zfabrik.impl.mvncr.util.LoggingTransferListener;
import com.zfabrik.util.fs.FileUtils;

/**
 * This is strongly inspired from 
 * 
 * http://git.eclipse.org/c/aether/aether-ant.git/tree/src/main/java/org/eclipse/aether/ant/AntRepoSys.jav
 * 
 * Note... the hard part is not only to dig through endless useless layers of abstraction but also to 
 * actually fill the missing links between parsing settings and making use of them
 * 
 * @author hb
 */
public class RepositoryAccess {
	private static final SettingsBuilder settingsBuilder = new DefaultSettingsBuilderFactory().newInstance();
	private static final SettingsDecrypter settingsDecrypter = new DefaultSettingsDecrypter();
    private static final ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();

	private Settings settings;
	private DefaultServiceLocator locator;
	private RepositorySystem repoSystem;
	private List<RemoteRepository> remoteRepos;
	private File localRepository;
	
	private boolean offline = false;

	/**
	 * We take information from settings.xml, in particular the remote repositories.
	 */
	public RepositoryAccess(File localRepo, File settingsFile) {
		
		this.localRepository = localRepo;
		// initialize services
        locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.setErrorHandler(new DefaultServiceLocator.ErrorHandler() {
        	@Override
        	public void serviceCreationFailed(Class<?> type, Class<?> impl,Throwable exception) {
        		throw new RuntimeException("Failed to create service of type "+type+" with implementation "+impl,exception);
        	}
		});
        
        locator.setServices( ModelBuilder.class, modelBuilder );
        locator.addService( RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class );
        locator.addService( TransporterFactory.class, FileTransporterFactory.class );
        locator.addService( TransporterFactory.class, HttpTransporterFactory.class );
        locator.addService( TransporterFactory.class, ClasspathTransporterFactory.class );
		
        DefaultSettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
        request.setGlobalSettingsFile(settingsFile);
        try {
            settings = settingsBuilder.build( request ).getEffectiveSettings();
        } catch ( SettingsBuildingException e ) {
            throw new IllegalStateException("Failed to process settings file "+settingsFile);
        }
        SettingsDecryptionResult result = settingsDecrypter.decrypt( new DefaultSettingsDecryptionRequest( settings ) );
        settings.setServers( result.getServers() );
        settings.setProxies( result.getProxies() );
	}
	
	/**
	 * In cases of erros in "relaxed" repos, we may decide to go offline
	 */
	public void setOffline(boolean offline) {
		this.offline = offline;
	}
	
	/**
	 * Create a repository system
	 * @return
	 */
	public synchronized RepositorySystem getSystem() {
		if (repoSystem == null) {
			repoSystem = locator.getService(RepositorySystem.class);
			if (repoSystem == null) {
				throw new IllegalStateException("Failed to access the aether repository system");
			}
		}
		return repoSystem ;
	}
	
	/**
	 * Create a repository system session (this is the state object for interacting with aether)
	 */
    public RepositorySystemSession getSystemSession() {
        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

        Map<Object, Object> configProps = new LinkedHashMap<Object, Object>();
        configProps.put( ConfigurationProperties.USER_AGENT, "com.zfabrik.mvncr");

        // apply the settings
        applySettings(configProps);
        session.setConfigProperties( configProps );

        session.setOffline( this.offline );

        session.setProxySelector( getProxySelector() );
        session.setMirrorSelector( getMirrorSelector() );
        session.setAuthenticationSelector( getAuthSelector() );

        session.setCache( new DefaultRepositoryCache() );

        session.setRepositoryListener(new LoggingRepositoryListener());
        session.setTransferListener(new LoggingTransferListener());

        // some stuff is hard coded
        LocalRepository localRepo = new LocalRepository(this.localRepository); 
        
        session.setLocalRepositoryManager( 
    		getSystem().newLocalRepositoryManager(session, localRepo)
    	);
        
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(ResolutionErrorPolicy.CACHE_NOT_FOUND));
        
        session.setUpdatePolicy(null);
        return session;
    }
   
    /**
     * Return all remote repos configured
     */
    public synchronized List<RemoteRepository> getRemoteRepositories() {
    	if (this.remoteRepos == null) {
			List<RemoteRepository> rr = new LinkedList<RemoteRepository>();
			for (Profile p : this.settings.getProfiles()) {
				for (Repository r : p.getRepositories()) {
					RemoteRepository.Builder b = new RemoteRepository.Builder(r.getId(),"default", r.getUrl());
					RepositoryPolicy repp = r.getReleases();
					if (repp!=null) {
						b.setReleasePolicy(new org.eclipse.aether.repository.RepositoryPolicy(repp.isEnabled(),repp.getUpdatePolicy(),repp.getChecksumPolicy()));
					}
					RepositoryPolicy sspp = r.getSnapshots();
					if (sspp!=null) {
						b.setSnapshotPolicy(new org.eclipse.aether.repository.RepositoryPolicy(sspp.isEnabled(),sspp.getUpdatePolicy(),sspp.getChecksumPolicy()));
					}
					rr.add(b.build());
				}
			}
			this.remoteRepos = rr;
    	}
    	return this.remoteRepos;
	}
    
    /**
     * fix auth
     */
    private AuthenticationSelector getAuthSelector() {
        DefaultAuthenticationSelector selector = new DefaultAuthenticationSelector();

        for ( Server server : settings.getServers() ) {
            AuthenticationBuilder auth = new AuthenticationBuilder();
            auth.addUsername( server.getUsername() ).addPassword( server.getPassword() );
            auth.addPrivateKey( server.getPrivateKey(), server.getPassphrase() );
            selector.add( server.getId(), auth.build() );
        }

        return new ConservativeAuthenticationSelector( selector );
    }


    /**
     * set mirrors
     * @return
     */
    private MirrorSelector getMirrorSelector() {
        DefaultMirrorSelector selector = new DefaultMirrorSelector();

        for ( Mirror mirror : settings.getMirrors() ) {
            selector.add( 
        		String.valueOf( mirror.getId() ), 
        		mirror.getUrl(), 
        		mirror.getLayout(), 
        		false,
                mirror.getMirrorOf(), 
                mirror.getMirrorOfLayouts() 
            );
        }

        return selector;
    }


    
    /**
     * set proxies
     */
	private ProxySelector getProxySelector() {
		DefaultProxySelector selector = new DefaultProxySelector();
		for (Proxy proxy : settings.getProxies()) {
			AuthenticationBuilder auth = new AuthenticationBuilder();
			auth.addUsername(proxy.getUsername()).addPassword(proxy.getPassword());
			selector.add(
				new org.eclipse.aether.repository.Proxy(
					proxy.getProtocol(), 
					proxy.getHost(), 
					proxy.getPort(), 
					auth.build()
				), 
				proxy.getNonProxyHosts()
		    );
		}
		return selector;
	}

    /**
     * Distill config options from parsed settings (WTF: Why is that not in aether?)
     * @param configProps
     */
	private void applySettings(Map<Object, Object> configProps) {
		for (Server server : settings.getServers()) {
			if (server.getConfiguration() != null) {
				Xpp3Dom dom = (Xpp3Dom) server.getConfiguration();
				for (int i = dom.getChildCount() - 1; i >= 0; i--) {
					Xpp3Dom child = dom.getChild(i);
					if ("wagonProvider".equals(child.getName())) {
						dom.removeChild(i);
					} else 
					if ("httpHeaders".equals(child.getName())) {
						configProps.put(ConfigurationProperties.HTTP_HEADERS+ "." + server.getId(), getHttpHeaders(child));
					}
				}

				configProps.put("aether.connector.wagon.config." + server.getId(), dom);
			}

			configProps.put("aether.connector.perms.fileMode." + server.getId(),server.getFilePermissions());
			configProps.put("aether.connector.perms.dirMode." + server.getId(),server.getDirectoryPermissions());
		}
	}
	
	/**
	 * Extract HTTP headers 
	 */
	private Map<String, String> getHttpHeaders(Xpp3Dom dom) {
		Map<String, String> headers = new HashMap<String, String>();
		for (int i = 0; i < dom.getChildCount(); i++) {
			Xpp3Dom child = dom.getChild(i);
			Xpp3Dom name = child.getChild("name");
			Xpp3Dom value = child.getChild("value");
			if (name != null && name.getValue() != null) {
				headers.put(name.getValue(), (value != null) ? value.getValue()
						: null);
			}
		}
		return headers;
	}

	public void clear() {
		FileUtils.delete(this.localRepository);
	}

	
	
}
