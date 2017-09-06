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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.talend.core.model.general.Project;
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

    public static boolean checkCycleReference(Project project) {
        List<ProjectReference> referenceList = project.getProjectReferenceList();
        if (referenceList.size() == 0) {
            return false;
        }
        Map<String, List<String>> referenceMap = new HashMap<String, List<String>>();
        List<String> list = new ArrayList<String>();
        referenceMap.put(project.getTechnicalLabel(), list);
        for (ProjectReference projetReference : referenceList) {
            list.add(projetReference.getReferencedProject().getTechnicalLabel());
            List<ProjectReference> childReferenceList = new Project(projetReference.getReferencedProject())
                    .getProjectReferenceList();
            if (childReferenceList.size() > 0) {
                List<String> childList = new ArrayList<String>();
                referenceMap.put(projetReference.getReferencedProject().getTechnicalLabel(), childList);
                for (ProjectReference pr : childReferenceList) {
                    childList.add(pr.getReferencedProject().getTechnicalLabel());
                }
            }
        }

        return checkCycleReference(referenceMap);
    }

    /**
     * 
     * @param referenceMap
     * @return false- cycle reference exist otherwise true
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean checkCycleReference(Map<String, List<String>> referenceMap) {
        List<String> allReferenceList = new ArrayList<String>();
        for (String key : referenceMap.keySet()) {
            if (!allReferenceList.contains(key)) {
                allReferenceList.add(key);
            }
            List<String> referenceList = referenceMap.get(key);
            for (String reference : referenceList) {
                if (!allReferenceList.contains(reference)) {
                    allReferenceList.add(reference);
                }
            }
        }
        List<int[]> prerequisites = new ArrayList<int[]>();
        for (String key : referenceMap.keySet()) {
            int keyId = allReferenceList.indexOf(key);
            List<String> referenceList = referenceMap.get(key);
            for (String reference : referenceList) {
                int refernceId = allReferenceList.indexOf(reference);
                prerequisites.add(new int[] { keyId, refernceId });
            }
        }

        List[] graph = new List[allReferenceList.size()];
        for (int i = 0; i < allReferenceList.size(); i++) {
            graph[i] = new ArrayList<Integer>();
        }
        boolean[] visited = new boolean[allReferenceList.size()];
        for (int i = 0; i < prerequisites.size(); i++) {
            graph[prerequisites.get(i)[1]].add(prerequisites.get(i)[0]);
        }

        for (int i = 0; i < allReferenceList.size(); i++) {
            if (!deepFirstSearch(graph, visited, i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean deepFirstSearch(List[] graph, boolean[] visited, int id) {
        if (visited[id]) {
            return false;
        } else {
            visited[id] = true;
        }

        for (int i = 0; i < graph[id].size(); i++) {
            if (!deepFirstSearch(graph, visited, (int) graph[id].get(i))) {
                return false;
            }
        }
        visited[id] = false;
        return true;
    }
}
