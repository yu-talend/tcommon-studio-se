package org.talend.core.repository.model;

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
import java.util.List;

import org.talend.commons.exception.InvalidProjectException;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.properties.ProjectReference;

public interface IReferenceProjectProvider {

    public static final String LABEL_TECHNICAL_LABEL = "technical_label";

    public static final String LABEL_BRANCH = "branch";

    public List<ProjectReference> getProjectReference();

    public void setProjectReference(List<ProjectReference> projectReferenceList);

    public void loadProjectReferenceSetting() throws Exception;

    public void saveProjectReferenceSetting() throws Exception;

    public void initReferenceProjectSetting(org.talend.core.model.general.Project[] allProjects)
            throws InvalidProjectException, PersistenceException;
}
