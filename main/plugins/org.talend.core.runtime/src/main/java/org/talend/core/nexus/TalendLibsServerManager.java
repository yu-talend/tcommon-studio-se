// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.core.nexus;

import java.util.Dictionary;
import java.util.Hashtable;

import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.service.cm.ConfigurationException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.properties.User;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.core.service.IRemoteService;
import org.talend.repository.model.IProxyRepositoryFactory;
import org.talend.repository.model.RepositoryConstants;

/**
 * created by wchen on 2015年6月16日 Detailled comment
 *
 */
public class TalendLibsServerManager {

    private static String NEXUS_USER = "nexus.user";

    private static String NEXUS_PASSWORD = "nexus.password";

    private static String NEXUS_URL = "nexus.url";

    private static String NEXUS_LIB_REPO = "nexus.lib.repo";

    private static String DEFAULT_LIB_REPO = "talend-custom-libs-release";

    private static String NEXUS_LIB_SNAPSHOT_REPO = "nexus.lib.repo.snapshot";

    private static String DEFAULT_LIB_SNAPSHOT_REPO = "talend-custom-libs-snapshot";

    public static final String KEY_NEXUS_RUL = "url";//$NON-NLS-1$

    public static final String KEY_NEXUS_USER = "username";//$NON-NLS-1$

    public static final String KEY_NEXUS_PASS = "password";//$NON-NLS-1$

    public static final String KEY_CUSTOM_LIB_REPOSITORY = "repositoryReleases";//$NON-NLS-1$

    public static final String KEY_CUSTOM_LIB_SNAPSHOT_REPOSITORY = "repositorySnapshots";//$NON-NLS-1$

    public static final String KEY_SOFTWARE_UPDATE_REPOSITORY = "repositoryID";//$NON-NLS-1$

    public static final String TALEND_LIB_SERVER = "https://talend-update.talend.com/nexus/";//$NON-NLS-1$

    public static final String TALEND_LIB_USER = "";//$NON-NLS-1$

    public static final String TALEND_LIB_PASSWORD = "";//$NON-NLS-1$

    public static final String TALEND_LIB_REPOSITORY = "libraries";//$NON-NLS-1$

    private static TalendLibsServerManager manager = null;

    private NexusServerBean artifactServerBean;

    public static final int CONNECTION_OK = 200;

    public static synchronized TalendLibsServerManager getInstance() {
        if (manager == null) {
            manager = new TalendLibsServerManager();
        }
        return manager;
    }

    public void updateMavenResolver(Dictionary<String, String> props, boolean setupCustomNexusFromTAC) {
        if (props == null) {
            props = new Hashtable<String, String>();
        }
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
            props.put(ServiceConstants.PID + '.' + ServiceConstants.PROPERTY_REPOSITORIES, repositories);
        }

        try {
            TalendMavenResolver.updateMavenResolver(props);
        } catch (ConfigurationException e) {
            throw new RuntimeException("Failed to modifiy the service properties"); //$NON-NLS-1$
        }

    }

    public NexusServerBean getCustomNexusServer() {
        if (!org.talend.core.PluginChecker.isCoreTISPluginLoaded()) {
            return null;
        }
        if (artifactServerBean == null) {
            try {
                String nexus_url = System.getProperty(NEXUS_URL);
                String nexus_user = System.getProperty(NEXUS_USER);
                String nexus_pass = System.getProperty(NEXUS_PASSWORD);
                String repositoryId = System.getProperty(NEXUS_LIB_REPO, DEFAULT_LIB_REPO);
                String snapshotRepId = System.getProperty(NEXUS_LIB_SNAPSHOT_REPO, DEFAULT_LIB_SNAPSHOT_REPO);

                IProxyRepositoryFactory factory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
                RepositoryContext repositoryContext = factory.getRepositoryContext();
                if ((nexus_url == null && (factory.isLocalConnectionProvider() || repositoryContext.isOffline()))) {
                    return null;
                }
                if (repositoryContext != null && repositoryContext.getFields() != null && !factory.isLocalConnectionProvider()
                        && !repositoryContext.isOffline()) {
                    String adminUrl = repositoryContext.getFields().get(RepositoryConstants.REPOSITORY_URL);
                    String userName = "";
                    String password = "";
                    User user = repositoryContext.getUser();
                    if (user != null) {
                        userName = user.getLogin();
                        password = repositoryContext.getClearPassword();
                    }

                    if (adminUrl != null && !"".equals(adminUrl)
                            && GlobalServiceRegister.getDefault().isServiceRegistered(IRemoteService.class)) {
                        IRemoteService remoteService = (IRemoteService) GlobalServiceRegister.getDefault().getService(
                                IRemoteService.class);
                        NexusServerBean bean = remoteService.getLibNexusServer(userName, password, adminUrl);
                        if (bean != null) {
                            nexus_url = bean.getServer();
                            nexus_user = bean.getUserName();
                            nexus_pass = bean.getPassword();
                            repositoryId = bean.getRepositoryId();
                            snapshotRepId = bean.getSnapshotRepId();
                            System.setProperty(NEXUS_URL, nexus_url);
                            System.setProperty(NEXUS_USER, nexus_user);
                            System.setProperty(NEXUS_PASSWORD, nexus_pass);
                            System.setProperty(NEXUS_LIB_REPO, repositoryId);
                            System.setProperty(NEXUS_LIB_SNAPSHOT_REPO, snapshotRepId);
                        }
                    }
                }
                artifactServerBean = new NexusServerBean();
                artifactServerBean.setServer(nexus_url);
                artifactServerBean.setUserName(nexus_user);
                artifactServerBean.setPassword(nexus_pass);
                artifactServerBean.setRepositoryId(repositoryId);
                artifactServerBean.setSnapshotRepId(snapshotRepId);
            } catch (Exception e) {
                artifactServerBean = null;
                ExceptionHandler.process(e);
            }
        }
        return artifactServerBean;

    }

    public NexusServerBean getTalentArtifactServer() {
        NexusServerBean serverBean = new NexusServerBean();
        serverBean.setServer(System.getProperty("org.talend.libraries.repo.url", TALEND_LIB_SERVER));
        serverBean.setUserName(TALEND_LIB_USER);
        serverBean.setPassword(TALEND_LIB_PASSWORD);
        serverBean.setRepositoryId(TALEND_LIB_REPOSITORY);
        serverBean.setOfficial(true);

        return serverBean;
    }

    public String resolveSha1(String nexusUrl, String userName, String password, String repositoryId, String groupId,
            String artifactId, String version, String type) throws Exception {
        return NexusServerUtils.resolveSha1(nexusUrl, userName, password, repositoryId, groupId, artifactId, version, type);
    }

    /**
     * 
     * DOC Talend Comment method "getSoftwareUpdateNexusServer". get nexus server configured in TAC for patch
     * 
     * @param adminUrl
     * @param userName
     * @param password
     * @return
     */
    public NexusServerBean getSoftwareUpdateNexusServer(String adminUrl, String userName, String password) {
        try {
            if (adminUrl != null && !"".equals(adminUrl)
                    && GlobalServiceRegister.getDefault().isServiceRegistered(IRemoteService.class)) {
                IRemoteService remoteService = (IRemoteService) GlobalServiceRegister.getDefault().getService(
                        IRemoteService.class);
                NexusServerBean serverBean = remoteService.getUpdateRepositoryUrl(userName, password, adminUrl);
                String nexus_url = serverBean.getServer();
                String nexus_user = serverBean.getUserName();
                String nexus_pass = serverBean.getPassword();
                String nexus_repository = serverBean.getRepositoryId();
                boolean connectionOK = NexusServerUtils
                        .checkConnectionStatus(nexus_url, nexus_repository, nexus_user, nexus_pass);
                if (!connectionOK) {
                    return null;
                }
                return serverBean;
            }
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        } catch (LoginException e) {
            ExceptionHandler.process(e);
        }

        return null;
    }

}
