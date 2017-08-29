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
package org.talend.designer.maven.tools;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.model.Model;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.talend.core.model.general.Project;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.tools.creator.CreateMavenBeanPom;
import org.talend.designer.maven.tools.creator.CreateMavenPigUDFPom;
import org.talend.designer.maven.tools.creator.CreateMavenRoutinePom;
import org.talend.designer.maven.utils.PomUtil;

/**
 * DOC zwxue class global comment. Detailled comment
 */
public class AggregatorPomsManager {

    private Project project;

    public AggregatorPomsManager(Project project) {
        this.project = project;
    }

    public void createRootPom(IFile pomFile, IProgressMonitor monitor) throws Exception {
        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(MavenTemplateManager.KEY_PROJECT_NAME, project.getTechnicalLabel());
        Model model = MavenTemplateManager.getCodeProjectTemplateModel(parameters);
        PomUtil.savePom(monitor, model, pomFile);
    }

    public void createAggregatorFolderPom(IFile pomFile, String folderName, String groupId, IProgressMonitor monitor)
            throws Exception {
        Model model = MavenTemplateManager.getAggregatorFolderTemplateModel(pomFile, groupId, folderName,
                project.getTechnicalLabel());
        PomUtil.savePom(monitor, model, pomFile);
    }

    public void createRoutinesPom(IFile pomFile, IProgressMonitor monitor) throws Exception {
        CreateMavenRoutinePom createTemplatePom = new CreateMavenRoutinePom(pomFile);
        createTemplatePom.create(monitor);
    }

    public void createPigUDFsPom(IFile pomFile, IProgressMonitor monitor) throws Exception {
        CreateMavenPigUDFPom createTemplatePom = new CreateMavenPigUDFPom(pomFile);
        createTemplatePom.create(monitor);
    }

    public void createBeansPom(IFile pomFile, IProgressMonitor monitor) throws Exception {
        CreateMavenBeanPom createTemplatePom = new CreateMavenBeanPom(pomFile);
        createTemplatePom.create(monitor);
    }
    
    public void createUserDefinedFolderPom(IFile pomFile, String folderName, String groupId, IProgressMonitor monitor) {
        // TODO get model like createAggregatorFolderPom(), but calculate parent's relative path.
        // PomUtil.getPomRelativePath(container, baseFolder);
    }

}
