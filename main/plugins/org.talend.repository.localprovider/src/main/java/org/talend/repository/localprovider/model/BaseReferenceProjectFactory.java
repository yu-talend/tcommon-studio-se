package org.talend.repository.localprovider.model;

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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.impl.PropertiesFactoryImpl;
import org.talend.core.repository.model.IReferenceProjectFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseReferenceProjectFactory implements IReferenceProjectFactory {

    private ReferenceProjectConfig referenceProjectConfig;

    @Override
    public List<ProjectReference> getProjectReference(Project project, String branchName) {
        List<ProjectReference> list = new ArrayList<ProjectReference>();
        if (referenceProjectConfig != null && referenceProjectConfig.getReference_project() != null) {
            PropertiesFactory propertiesFactory = PropertiesFactoryImpl.init();
            for (Map<String, String> map : referenceProjectConfig.getReference_project()) {
                ProjectReference pr = propertiesFactory.createProjectReference();
                pr.setProject(project);
                pr.setBranch(branchName);
                pr.setReferencedBranch(map.get(LABEL_BRANCH));
                Project rp = propertiesFactory.createProject();
                rp.setTechnicalLabel(map.get(LABEL_TECHNICAL_LABEL));
                pr.setReferencedProject(rp);

                list.add(pr);
            }

        }
        return list;
    }

    @Override
    public void setProjectReference(Project project, String branchName, List<ProjectReference> projectReferenceList) {

        if (projectReferenceList == null || projectReferenceList.size() == 0) {
            return;
        }
        if (referenceProjectConfig != null && referenceProjectConfig.getReference_project() != null) {
            referenceProjectConfig.getReference_project().clear();
        }
        if (referenceProjectConfig == null) {
            referenceProjectConfig = new ReferenceProjectConfig();
        }
        for (ProjectReference projectReference : projectReferenceList) {
            Map<String, String> map = new HashMap<String, String>();
            map.put(LABEL_TECHNICAL_LABEL, projectReference.getReferencedProject().getTechnicalLabel());
            map.put(LABEL_TECHNICAL_LABEL, projectReference.getReferencedBranch());
            referenceProjectConfig.getReference_project().add(map);
        }
    }

    @Override
    public void loadProjectReferenceSetting(Project project, String branchName) {
        TypeReference<ReferenceProjectConfig> typeReference = new TypeReference<ReferenceProjectConfig>() {
            // no need to overwrite
        };

        try {
            URL url = getFileURL(project, branchName);
            if (url != null) {
                referenceProjectConfig = new ObjectMapper().readValue(getFileURL(project, branchName), typeReference);
            }
        } catch (Throwable e) {
            ExceptionHandler.process(e);
        }
    }

    protected URL getFileURL(Project project, String branchName) throws PersistenceException, MalformedURLException {
        IProject iProject = ResourceUtils.getProject(project.getTechnicalLabel());
        IFolder folder = iProject.getFolder(".settings");
        IFile file = folder.getFile("references.properties");
        if (file != null) {
            File propertiesFile = new File(file.getLocationURI());
            if (propertiesFile != null && propertiesFile.exists()) {
                return file.getLocationURI().toURL();
            }
        }
        return null;
    }

    @Override
    public void saveProjectReferenceSetting(Project project, String branchName) throws Exception {
        URL url = getFileURL(project, branchName);
        File file = new File(url.getPath());
        ObjectMapper objectMapper = new ObjectMapper();
        if (referenceProjectConfig == null) {
            referenceProjectConfig = new ReferenceProjectConfig();
        }
        String content = objectMapper.writeValueAsString(referenceProjectConfig);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
        } finally {
            bw.flush();
            bw.close();
        }
    }
}

class ReferenceProjectConfig {

    private List<Map<String, String>> reference_project = new ArrayList<Map<String, String>>();

    public List<Map<String, String>> getReference_project() {
        return reference_project;
    }

    public void setReference_project(List<Map<String, String>> reference_project) {
        this.reference_project = reference_project;
    }

}
