package org.talend.repository;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.talend.commons.exception.BusinessException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.repository.model.IProxyRepositoryFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReferenceProjectProvider implements IReferenceProjectProvider {

    private Project project;

    private byte[] configContent = null;

    private boolean loadFromContent = false;

    private ReferenceProjectConfiguration referenceProjectConfig;

    private List<ProjectReference> referenceProjectList = new ArrayList<ProjectReference>();

    private boolean hasConfigurationFile = false;

    private static Map<String, List<ProjectReference>> tempReferenceMap = new HashMap<String, List<ProjectReference>>();

    private static Map<String, List<ProjectReference>> tacReferenceMap = new HashMap<String, List<ProjectReference>>();

    public ReferenceProjectProvider(Project project) {
        this.project = project;
    }

    public ReferenceProjectProvider(Project project, byte[] configContent) {
        this.project = project;
        this.configContent = configContent;
        loadFromContent = true;
    }

    public void initSettings() throws BusinessException, PersistenceException {
        IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();

        loadSettings();
        if (referenceProjectConfig != null) {
            for (ReferenceProjectBean bean : referenceProjectConfig.getReferenceProject()) {
                org.talend.core.model.properties.Project refProject = proxyRepositoryFactory
                        .getEmfProjectContent(bean.getProjectTechnicalName());
                if (refProject != null) {
                    if (!project.getTechnicalLabel().equals(refProject.getTechnicalLabel())) {
                        ProjectReference projectReference = PropertiesFactory.eINSTANCE.createProjectReference();
                        projectReference.setReferencedProject(refProject);
                        projectReference.setReferencedBranch(bean.getBranchName());
                        referenceProjectList.add(getProjectReferenceInstance(refProject, bean));
                    }
                } else {
                    ReferenceProjectProblemManager.getInstance().addInvalidProjectReference(bean.getProjectTechnicalName(),
                            bean.getBranchName());
                }
            }
        }
        return;
    }

    private ProjectReference getProjectReferenceInstance(org.talend.core.model.properties.Project refProject,
            ReferenceProjectBean bean) {
        ProjectReference pr = PropertiesFactory.eINSTANCE.createProjectReference();
        pr.setReferencedBranch(bean.getBranchName());
        pr.setReferencedProject(refProject);
        return pr;
    }

    @Override
    public List<ProjectReference> getProjectReference() throws PersistenceException {
        if (!loadFromContent && tempReferenceMap.get(project.getTechnicalLabel()) != null) {
            return getTempReferenceList(project.getTechnicalLabel());
        }
        if (!loadFromContent && !isHasConfigurationFile() && getTacReferenceList(project.getTechnicalLabel()) != null) {
            return getTacReferenceList(project.getTechnicalLabel());
        }
        return referenceProjectList;
    }

    @Override
    public void setProjectReference(List<ProjectReference> projectReferenceList) {
        if (referenceProjectConfig != null && referenceProjectConfig.getReferenceProject() != null) {
            referenceProjectConfig.getReferenceProject().clear();
        }
        if (referenceProjectConfig == null) {
            referenceProjectConfig = new ReferenceProjectConfiguration();
        }
        if (projectReferenceList == null || projectReferenceList.size() == 0) {
            return;
        }
        for (ProjectReference projectReference : projectReferenceList) {
            ReferenceProjectBean bean = new ReferenceProjectBean();
            bean.setProjectTechnicalName(projectReference.getReferencedProject().getTechnicalLabel());
            bean.setBranchName(projectReference.getReferencedBranch());
            referenceProjectConfig.getReferenceProject().add(bean);
        }
    }

    @Override
    public void loadSettings() throws PersistenceException {
        TypeReference<ReferenceProjectConfiguration> typeReference = new TypeReference<ReferenceProjectConfiguration>() {
            // no need to overwrite
        };

        try {
            if (loadFromContent) {
                referenceProjectConfig = new ObjectMapper().readValue(configContent, typeReference);
            } else {
                File file = getConfigurationFile();
                if (file != null && file.exists()) {
                    hasConfigurationFile = true;
                    referenceProjectConfig = new ObjectMapper().readValue(file, typeReference);
                }
            }
        } catch (Throwable e) {
            throw new PersistenceException(e);
        }
    }

    protected File getConfigurationFile() throws PersistenceException  {
        IProject iProject = ResourceUtils.getProject(project.getTechnicalLabel());
        IFolder folder = iProject.getFolder(CONFIGURATION_FOLDER_NAME);
        IFile file = folder.getFile(CONFIGURATION_FILE_NAME);
        File propertiesFile = new File(file.getLocationURI());
        return propertiesFile;
    }

    @Override
    public void saveSettings() throws PersistenceException, IOException  {
        File file = getConfigurationFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (referenceProjectConfig == null) {
            referenceProjectConfig = new ReferenceProjectConfiguration();
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(referenceProjectConfig);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(content);
        } finally {
            bw.flush();
            bw.close();
        }
    }

    public boolean isHasConfigurationFile() {
        return hasConfigurationFile;
    }

    public static void setTempReferenceList(String projectLabel, List<ProjectReference> referenceList) {
        tempReferenceMap.put(projectLabel, referenceList);
    }

    public static List<ProjectReference> getTempReferenceList(String projectLabel) throws PersistenceException {
        List<ProjectReference> referenceList = tempReferenceMap.get(projectLabel);
        if (referenceList != null) {
            List<ProjectReference> clonedList = new ArrayList<ProjectReference>();
            IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
            for (ProjectReference pr : referenceList) {
                org.talend.core.model.properties.Project refProject = proxyRepositoryFactory
                        .getEmfProjectContent(pr.getReferencedProject().getTechnicalLabel());
                if (refProject != null) {
                    pr.setReferencedProject(refProject);
                }
                clonedList.add(pr);
            }
            return clonedList;
        }
        return null;
    }

    public static void removeAllTempReferenceList() {
        tempReferenceMap.clear();
    }

    public static void setTacReferenceList(String projectLabel, List<ProjectReference> referenceList) {
        tacReferenceMap.put(projectLabel, referenceList);
    }

    public static List<ProjectReference> getTacReferenceList(String projectLabel) throws PersistenceException {
        List<ProjectReference> referenceList = tacReferenceMap.get(projectLabel);
        if (referenceList != null) {
            List<ProjectReference> clonedList = new ArrayList<ProjectReference>();
            IProxyRepositoryFactory proxyRepositoryFactory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
            for (ProjectReference pr : referenceList) {
                org.talend.core.model.properties.Project refProject = proxyRepositoryFactory
                        .getEmfProjectContent(pr.getReferencedProject().getTechnicalLabel());
                if (refProject != null) {
                    pr.setReferencedProject(refProject);
                }
                clonedList.add(pr);
            }
            return clonedList;
        }
        return null;
    }

    public static void removeAllTacReferenceList() {
        tacReferenceMap.clear();
    }
}

class ReferenceProjectConfiguration {

    @JsonProperty("reference_projects")
    private List<ReferenceProjectBean> referenceProject = new ArrayList<ReferenceProjectBean>();

    public List<ReferenceProjectBean> getReferenceProject() {
        return referenceProject;
    }

    public void setReferenceProject(List<ReferenceProjectBean> referenceProject) {
        this.referenceProject = referenceProject;
    }
}

class ReferenceProjectBean {

    @JsonProperty("project")
    private String projectTechnicalName;

    @JsonProperty("branch")
    private String branchName;

    public String getProjectTechnicalName() {
        return projectTechnicalName;
    }

    public void setProjectTechnicalName(String projectTechnicalName) {
        this.projectTechnicalName = projectTechnicalName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
