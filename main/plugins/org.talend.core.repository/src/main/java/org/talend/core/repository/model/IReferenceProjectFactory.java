package org.talend.core.repository.model;
//============================================================================
//
//Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//This source code is available under agreement available at
//%InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
//You should have received a copy of the agreement
//along with this program; if not, write to Talend SA
//9 rue Pages 92150 Suresnes, France
//
//============================================================================
import java.util.List;

import org.talend.core.model.properties.Project;
import org.talend.core.model.properties.ProjectReference;

public interface IReferenceProjectFactory {
    public static final String LABEL_TECHNICAL_LABEL = "technical_label";
    public static final String LABEL_BRANCH = "branch";

    public List<ProjectReference> getProjectReference(Project project, String branchName);

    public void setProjectReference(Project project, String branchName, List<ProjectReference> projectReferenceList);

    public void loadProjectReferenceSetting(Project project, String branchName) throws Exception;

    public void saveProjectReferenceSetting(Project project, String branchName) throws Exception;
}
