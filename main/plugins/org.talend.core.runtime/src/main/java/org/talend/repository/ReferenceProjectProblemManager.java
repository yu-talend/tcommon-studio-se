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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.talend.core.model.properties.ProjectReference;

public class ReferenceProjectProblemManager {

    private static ReferenceProjectProblemManager instance;

    private Map<String, String> invalidProjectMap = new HashMap<String, String>();

    public static synchronized ReferenceProjectProblemManager getInstance() {
        if (instance == null) {
            instance = new ReferenceProjectProblemManager();
        }
        return instance;
    }

    public void addInvalidProjectReference(ProjectReference projectReference) {
        if (projectReference != null) {
            invalidProjectMap.put(projectReference.getReferencedProject().getTechnicalLabel(),
                    projectReference.getReferencedBranch());
        }
    }

    public Set<String> getInvalidProjectReferenceSet() {
        return invalidProjectMap.keySet();
    }

    public void clearAll() {
        invalidProjectMap.clear();
    }
}
