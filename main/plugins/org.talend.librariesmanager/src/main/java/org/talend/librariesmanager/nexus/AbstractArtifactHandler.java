// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.nexus;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.talend.core.nexus.IArtifactHandler;
import org.talend.core.nexus.NexusConstants;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.runtime.maven.MavenArtifact;

/**
 * created by wchen on Aug 2, 2017 Detailled comment
 *
 */
public abstract class AbstractArtifactHandler implements IArtifactHandler {

    private MavenResolver mavenResolver = null;

    /**
     * DOC wchen AbstractArtifactHandler constructor comment.
     */
    public AbstractArtifactHandler() {
        // the tracker is use in case the service is modifed
        final BundleContext context = CoreRuntimePlugin.getInstance().getBundle().getBundleContext();
        ServiceTracker<org.ops4j.pax.url.mvn.MavenResolver, org.ops4j.pax.url.mvn.MavenResolver> serviceTracker = new ServiceTracker<org.ops4j.pax.url.mvn.MavenResolver, org.ops4j.pax.url.mvn.MavenResolver>(
                context, org.ops4j.pax.url.mvn.MavenResolver.class,
                new ServiceTrackerCustomizer<org.ops4j.pax.url.mvn.MavenResolver, org.ops4j.pax.url.mvn.MavenResolver>() {

                    @Override
                    public org.ops4j.pax.url.mvn.MavenResolver addingService(
                            ServiceReference<org.ops4j.pax.url.mvn.MavenResolver> reference) {
                        return context.getService(reference);
                    }

                    @Override
                    public void modifiedService(ServiceReference<org.ops4j.pax.url.mvn.MavenResolver> reference,
                            org.ops4j.pax.url.mvn.MavenResolver service) {
                        mavenResolver = null;

                    }

                    @Override
                    public void removedService(ServiceReference<org.ops4j.pax.url.mvn.MavenResolver> reference,
                            org.ops4j.pax.url.mvn.MavenResolver service) {
                        mavenResolver = null;
                    }
                });
        serviceTracker.open();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactRepositoryHandler#checkConnection(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public boolean checkConnection(String nexusUrl, String repositoryId, String userName, String password) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactRepositoryHandler#search(java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<MavenArtifact> search(String nexusUrl, String userName, String password, String repositoryId,
            String groupIdToSearch, String artifactId, String versionToSearch) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactRepositoryHandler#install(java.io.File,
     * org.talend.core.runtime.maven.MavenArtifact, java.lang.String)
     */
    @Override
    public void install(File content, MavenArtifact artifact, String type) throws Exception {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactRepositoryHandler#resolve()
     */
    @Override
    public File resolve(String mvnUrl) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactHandler#upload(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.io.File)
     */
    @Override
    public void upload(String groupId, String artifactId, String classifier, String extension, String version, File artifact)
            throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateMavenResolver(Dictionary<String, String> props, boolean setupCustomNexusFromTAC) {
        if (props == null) {
            props = new Hashtable<String, String>();
        }
        final BundleContext context = CoreRuntimePlugin.getInstance().getBundle().getBundleContext();
        ServiceReference<ManagedService> managedServiceRef = context.getServiceReference(ManagedService.class);
        if (managedServiceRef != null) {
            if (setupCustomNexusFromTAC) {
                String repositories = "";
                NexusServerBean customServer = getCustomNexusServer();
                if (customServer != null) {
                    String custom_server = customServer.getServer();
                    String custom_user = customServer.getUserName();
                    String custom_pass = customServer.getPassword();
                    String release_rep = customServer.getRepositoryId();
                    String snapshot_rep = customServer.getSnapshotRepId();
                    if (custom_server.endsWith(NexusConstants.SLASH)) {
                        custom_server = custom_server.substring(0, custom_server.length() - 1);
                    }
                    if (custom_user != null && !"".equals(custom_user)) {//$NON-NLS-1$
                        String[] split = custom_server.split("://");//$NON-NLS-1$
                        if (split.length != 2) {
                            throw new RuntimeException("Nexus url is not valid ,please contract the administrator");
                        }
                        custom_server = split[0] + "://" + custom_user + ":" + custom_pass + "@"//$NON-NLS-1$
                                + split[1] + NexusConstants.CONTENT_REPOSITORIES;
                    }
                    String releaseUrl = custom_server + release_rep + "@id=" + release_rep;//$NON-NLS-1$
                    String snapshotUrl = custom_server + snapshot_rep + "@id=" + snapshot_rep + NexusConstants.SNAPSHOTS;//$NON-NLS-1$
                    // custom nexus server should use snapshot repository
                    repositories = releaseUrl + "," + snapshotUrl;
                }
                final NexusServerBean officailServer = getLibrariesNexusServer();
                String official_server = officailServer.getServer();
                // remove the trailing slash
                if (official_server.endsWith(NexusConstants.SLASH)) {
                    official_server = official_server.substring(0, official_server.length() - 1);
                }
                String officalUrl = official_server + NexusConstants.CONTENT_REPOSITORIES + officailServer.getRepositoryId()
                        + "@id=" + officailServer.getRepositoryId();//$NON-NLS-1$
                if (repositories.isEmpty()) {
                    repositories = officalUrl;
                } else {
                    repositories = repositories + "," + officalUrl;
                }
                props.put(ServiceConstants.PID + '.' + ServiceConstants.PROPERTY_REPOSITORIES, repositories);
            }
            ManagedService managedService = context.getService(managedServiceRef);

            try {
                managedService.updated(props);
                mavenResolver = null;
            } catch (ConfigurationException e) {
                throw new RuntimeException("Failed to modifiy the service properties"); //$NON-NLS-1$
            }
        } else {
            throw new RuntimeException("Failed to load the service :" + ManagedService.class.getCanonicalName()); //$NON-NLS-1$
        }

    }

    public MavenResolver getMavenResolver() throws RuntimeException {
        if (mavenResolver == null) {
            final BundleContext context = CoreRuntimePlugin.getInstance().getBundle().getBundleContext();
            ServiceReference<org.ops4j.pax.url.mvn.MavenResolver> mavenResolverService = context
                    .getServiceReference(org.ops4j.pax.url.mvn.MavenResolver.class);
            if (mavenResolverService != null) {
                mavenResolver = context.getService(mavenResolverService);
            } else {
                throw new RuntimeException("Unable to acquire org.ops4j.pax.url.mvn.MavenResolver");
            }
        }

        return mavenResolver;

    }

}
