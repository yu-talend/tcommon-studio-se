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
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.ecore.resource.Resource;
import org.talend.commons.exception.InvalidProjectException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.impl.PropertiesFactoryImpl;
import org.talend.core.repository.model.IReferenceProjectProvider;
import org.talend.core.repository.model.ReferenceProjectProblemManager;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseReferenceProjectProvider implements IReferenceProjectProvider {

    private static Logger log = Logger.getLogger(BaseReferenceProjectProvider.class);

    private Project project;

    private String branchName;

    private ReferenceProjectConfiguration referenceProjectConfig;

    public BaseReferenceProjectProvider(Project project, String branchName) {
        this.project = project;
        this.branchName = branchName;
    }

    @SuppressWarnings("unchecked")
    public void initReferenceProjectSetting(org.talend.core.model.general.Project[] allAvailableProjects)
            throws InvalidProjectException, PersistenceException {
        Resource projectResource = project.eResource();
        List<ProjectReference> referenceProjectList = project.getReferencedProjects();
        for (ProjectReference projectReference : referenceProjectList) {
            projectResource.getContents().remove(projectReference);
        }
        project.getReferencedProjects().clear();
        ReferenceProjectProblemManager.getInstance().clearAll();
        
        BaseReferenceProjectProvider factory = new BaseReferenceProjectProvider(project, branchName);
        factory.loadProjectReferenceSetting();
        List<ProjectReference> list = factory.getProjectReference();
        for (ProjectReference projectReference : list) {
            org.talend.core.model.general.Project refProject = findProjectByTechnicalLabel(allAvailableProjects,
                    projectReference.getReferencedProject().getTechnicalLabel());
            if (refProject != null) {
                projectReference.setProject(project);
                projectReference.setReferencedProject(refProject.getEmfProject());
                projectResource.getContents().add(projectReference);
            } else {
                ReferenceProjectProblemManager.getInstance().addInvalidProjectReference(projectReference);
                log.error("Invalid admin access: Can't access project {"
                        + projectReference.getReferencedProject().getTechnicalLabel() + "} by current user.");
            }
        }
        // get exception message
        if (ReferenceProjectProblemManager.getInstance().getInvalidProjectReferenceList().size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (ProjectReference pr : ReferenceProjectProblemManager.getInstance().getInvalidProjectReferenceList()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(pr.getReferencedProject().getTechnicalLabel());
            }
            throw new InvalidProjectException(" Can't access project {" + sb.toString() + "} by current user.");
        }
        return;
    }

    private org.talend.core.model.general.Project findProjectByTechnicalLabel(org.talend.core.model.general.Project[] allAvailableProjects,
            String technicalLabel) {
        if (allAvailableProjects != null) {
            for (org.talend.core.model.general.Project project : allAvailableProjects) {
                if (technicalLabel.equals(project.getTechnicalLabel())) {
                    return project;
                }
            }
        }
        return null;
    }

    @Override
    public List<ProjectReference> getProjectReference() {
        List<ProjectReference> list = new ArrayList<ProjectReference>();
        if (referenceProjectConfig != null && referenceProjectConfig.getReferenceProject() != null) {
            PropertiesFactory propertiesFactory = PropertiesFactoryImpl.init();
            for (ReferenceProjectBean bean : referenceProjectConfig.getReferenceProject()) {
                ProjectReference pr = propertiesFactory.createProjectReference();
                pr.setBranch(branchName);
                pr.setReferencedBranch(bean.getBranchName());
                Project rp = propertiesFactory.createProject();
                rp.setTechnicalLabel(bean.getProjectTechnicalName());
                pr.setReferencedProject(rp);
                list.add(pr);
            }
        }
        return list;
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
    public void loadProjectReferenceSetting() throws PersistenceException {
        TypeReference<ReferenceProjectConfiguration> typeReference = new TypeReference<ReferenceProjectConfiguration>() {
            // no need to overwrite
        };

        try {
            File file = getConfigurationFile();
            if (file != null && file.exists()) {
                referenceProjectConfig = new ObjectMapper().readValue(file, typeReference);
            }
        } catch (Throwable e) {
            throw new PersistenceException(e);
        }
    }

    protected File getConfigurationFile() throws Exception {
        IProject iProject = ResourceUtils.getProject(project.getTechnicalLabel());
        IFolder folder = iProject.getFolder(".settings"); //$NON-NLS-1$
        IFile file = folder.getFile("references.properties"); //$NON-NLS-1$
        File propertiesFile = new File(file.getLocationURI());
        return propertiesFile;
    }

    @Override
    public void saveProjectReferenceSetting() throws Exception {
        File file = getConfigurationFile();
        if (!file.exists()) {
            file.createNewFile();
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
}

class ReferenceProjectConfiguration {

    @JsonProperty("reference_project")
    private List<ReferenceProjectBean> referenceProject = new ArrayList<ReferenceProjectBean>();

    public List<ReferenceProjectBean> getReferenceProject() {
        return referenceProject;
    }

    public void setReferenceProject(List<ReferenceProjectBean> referenceProject) {
        this.referenceProject = referenceProject;
    }
}

class ReferenceProjectBean {
    @JsonProperty("technical_label")
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
