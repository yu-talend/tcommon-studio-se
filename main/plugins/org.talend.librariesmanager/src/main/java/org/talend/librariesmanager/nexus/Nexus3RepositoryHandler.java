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
import java.util.List;

import org.talend.core.nexus.IRepositoryArtifactHandler;
import org.talend.core.runtime.maven.MavenArtifact;

/**
 * created by wchen on Aug 2, 2017 Detailled comment
 *
 */
public class Nexus3RepositoryHandler extends AbstractArtifactRepositoryHandler {

    @Override
    public IRepositoryArtifactHandler clone() {
        return new Nexus3RepositoryHandler();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#checkConnection()
     */
    @Override
    public boolean checkConnection() {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#checkConnection(boolean, boolean)
     */
    @Override
    public boolean checkConnection(boolean checkRelease, boolean checkSnapshot) {
        // TODO Auto-generated method stub
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#search(java.lang.String, java.lang.String,
     * java.lang.String, boolean, boolean)
     */
    @Override
    public List<MavenArtifact> search(String groupIdToSearch, String artifactId, String versionToSearch, boolean fromRelease,
            boolean fromSnapshot) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#deploy(java.io.File, java.lang.String, java.lang.String,
     * java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void deploy(File content, String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IRepositoryArtifactHandler#resolve(java.lang.String)
     */
    @Override
    public File resolve(String mvnUrl) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

}
