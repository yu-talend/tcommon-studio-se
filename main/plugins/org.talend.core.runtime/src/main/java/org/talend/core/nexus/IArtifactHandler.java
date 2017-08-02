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
package org.talend.core.nexus;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.List;

import org.talend.core.runtime.maven.MavenArtifact;

/**
 * created by wchen on Jul 31, 2017 Detailled comment
 *
 */
public interface IArtifactHandler {

    public boolean checkConnection(String nexusUrl, String repositoryId, final String userName, final String password);

    public void updateMavenResolver(Dictionary<String, String> props, boolean setupCustomNexusFromTAC);

    public List<MavenArtifact> search(String nexusUrl, String userName, String password, String repositoryId,
            String groupIdToSearch, String artifactId, String versionToSearch) throws Exception;

    public void install(File content, MavenArtifact artifact, String type) throws Exception;

    public File resolve(String mvnUrl) throws Exception;

    public void upload(String groupId, String artifactId, String classifier, String extension, String version, File artifact)
            throws IOException;
}
