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

import java.util.ArrayList;
import java.util.List;

import org.talend.core.model.properties.ProjectReference;

public class ReferenceProjectProblemManager {

    private static ReferenceProjectProblemManager instance;

    private List<ProjectReference> invalidProjectReferenceList = new ArrayList<ProjectReference>();

    public static synchronized ReferenceProjectProblemManager getInstance() {
        if (instance == null) {
            instance = new ReferenceProjectProblemManager();
        }
        return instance;
    }

    public void addInvalidProjectReference(ProjectReference projectReference) {
        if (projectReference != null) {
            invalidProjectReferenceList.add(projectReference);
        }
    }

    public List<ProjectReference> getInvalidProjectReferenceList() {
        List<ProjectReference> result = new ArrayList<ProjectReference>();
        result.addAll(invalidProjectReferenceList);
        return result;
    }

    public void clearAll() {
        invalidProjectReferenceList.clear();
    }

    public boolean isValidProjectReference(ProjectReference projectReference) {
        if (projectReference != null) {
            for (ProjectReference pr : invalidProjectReferenceList) {
                if (projectReference.getReferencedProject().getTechnicalLabel()
                        .equals(pr.getReferencedProject().getTechnicalLabel())) {
                    return false;
                }
            }
        }
        return true;
    }
}
