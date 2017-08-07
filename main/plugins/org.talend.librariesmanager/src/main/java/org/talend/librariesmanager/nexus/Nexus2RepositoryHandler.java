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
import java.util.ArrayList;
import java.util.List;

import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.general.INexusService;
import org.talend.core.nexus.IRepositoryArtifactHandler;
import org.talend.core.nexus.NexusServerUtils;
import org.talend.core.runtime.maven.MavenArtifact;

/**
 * created by wchen on Aug 2, 2017 Detailled comment
 *
 */
public class Nexus2RepositoryHandler extends AbstractArtifactRepositoryHandler {

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactHandler#checkConnection(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String)
     */
    @Override
    public boolean checkConnection() {
        return checkConnection(true, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#checkConnection(boolean, boolean)
     */
    @Override
    public boolean checkConnection(boolean checkRelease, boolean checkSnapshot) {
        boolean releaseStatus = false;
        boolean snapshotStatus = false;
        if (checkRelease && serverBean.getRepositoryId() != null) {
            releaseStatus = NexusServerUtils.checkConnectionStatus(serverBean.getRepositoryURI(), serverBean.getRepositoryId(),
                    serverBean.getUserName(), serverBean.getPassword());
        }
        if (checkSnapshot && serverBean.getSnapshotRepId() != null) {
            snapshotStatus = NexusServerUtils.checkConnectionStatus(serverBean.getRepositoryURI(), serverBean.getSnapshotRepId(),
                    serverBean.getUserName(), serverBean.getPassword());
        }
        boolean result = (checkRelease ? releaseStatus : true) && (checkSnapshot ? snapshotStatus : true);
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactHandler#search(java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public List<MavenArtifact> search(String groupIdToSearch, String artifactId, String versionToSearch, boolean fromRelease,
            boolean fromSnapshot) throws Exception {
        List<MavenArtifact> results = new ArrayList<MavenArtifact>();
        if (fromRelease && serverBean.getRepositoryId() != null) {
            results.addAll(NexusServerUtils.search(serverBean.getRepositoryURI(), serverBean.getUserName(),
                    serverBean.getPassword(), serverBean.getRepositoryId(), groupIdToSearch, artifactId, versionToSearch));
        }
        if (fromSnapshot && serverBean.getSnapshotRepId() != null) {
            results.addAll(NexusServerUtils.search(serverBean.getRepositoryURI(), serverBean.getUserName(),
                    serverBean.getPassword(), serverBean.getSnapshotRepId(), groupIdToSearch, artifactId, versionToSearch));
        }
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifactHandler#install(java.io.File, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deploy(File content, String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        if (GlobalServiceRegister.getDefault().isServiceRegistered(INexusService.class)) {
            INexusService nexusService = (INexusService) GlobalServiceRegister.getDefault().getService(INexusService.class);
            nexusService.upload(serverBean, groupId, artifactId, version, content.toURI().toURL());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifacRepositoryHandler#resolve(java.lang.String)
     */
    @Override
    public File resolve(String mvnUrl) throws Exception {
        return null;
    }

    @Override
    public IRepositoryArtifactHandler clone() {
        return new Nexus2RepositoryHandler();
    }

}
