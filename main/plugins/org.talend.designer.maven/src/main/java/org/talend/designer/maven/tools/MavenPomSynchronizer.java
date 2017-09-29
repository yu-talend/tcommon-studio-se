// ============================================================================
//
// Copyright (C) 2006-2017 Talend Inc. - www.talend.com
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

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.model.Model;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.talend.commons.utils.generation.JavaUtils;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.model.process.IProcess;
import org.talend.core.model.process.ProcessUtils;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.runtime.process.ITalendProcessJavaProject;
import org.talend.core.runtime.process.TalendProcessArgumentConstant;
import org.talend.core.runtime.projectsetting.IProjectSettingPreferenceConstants;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.tools.creator.CreateMavenBeanPom;
import org.talend.designer.maven.tools.creator.CreateMavenPigUDFPom;
import org.talend.designer.maven.tools.creator.CreateMavenRoutinePom;
import org.talend.designer.maven.utils.PomUtil;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.IRunProcessService;
import org.talend.utils.io.FilesUtils;

/**
 * created by ggu on 2 Feb 2015 Detailled comment
 *
 */
public class MavenPomSynchronizer {

    private final ITalendProcessJavaProject codeProject;
    
    private IRunProcessService runProcessService;
    
    public MavenPomSynchronizer(IProcessor processor) {
        this(processor.getTalendJavaProject());
    }

    public MavenPomSynchronizer(ITalendProcessJavaProject codeProject) {
        super();
        this.codeProject = codeProject;
        if (GlobalServiceRegister.getDefault().isServiceRegistered(IRunProcessService.class)) {
            runProcessService = (IRunProcessService) GlobalServiceRegister.getDefault().getService(IRunProcessService.class);
        }
    }

    /**
     * generate routine pom.
     */
    public void syncRoutinesPom(Property property, boolean overwrite) throws Exception {
        ITalendProcessJavaProject routineProject = runProcessService.getTalendCodeJavaProject(ERepositoryObjectType.ROUTINES);
        IFile routinesPomFile = routineProject.getProjectPom();
        // generate new one
        CreateMavenRoutinePom createTemplatePom = new CreateMavenRoutinePom(routinesPomFile);
        createTemplatePom.setProperty(property);
        createTemplatePom.setOverwrite(overwrite);
        createTemplatePom.create(null);
        buildAndInstallCodesProject(routineProject, runProcessService.isExportConfig());
    }

    public void syncBeansPom(Property property, boolean overwrite) throws Exception {
        ITalendProcessJavaProject beansProject = runProcessService.getTalendCodeJavaProject(ERepositoryObjectType.valueOf("BEANS")); //$NON-NLS-1$
        IFile beansPomFile = beansProject.getProjectPom();
        // generate new one
        CreateMavenBeanPom createTemplatePom = new CreateMavenBeanPom(beansPomFile);
        createTemplatePom.setProperty(property);
        createTemplatePom.setOverwrite(overwrite);
        createTemplatePom.create(null);
        buildAndInstallCodesProject(beansProject, runProcessService.isExportConfig());
    }

    public void syncPigUDFsPom(Property property, boolean overwrite) throws Exception {
        ITalendProcessJavaProject pigudfsProject = runProcessService.getTalendCodeJavaProject(ERepositoryObjectType.PIG_UDF);
        IFile pigudfPomFile = pigudfsProject.getProjectPom();
        // generate new one
        CreateMavenPigUDFPom createTemplatePom = new CreateMavenPigUDFPom(pigudfPomFile);
        createTemplatePom.setProperty(property);
        createTemplatePom.setOverwrite(overwrite);
        createTemplatePom.create(null);
        buildAndInstallCodesProject(pigudfsProject, runProcessService.isExportConfig());
    }

    private void buildAndInstallCodesProject(ITalendProcessJavaProject codeProject, boolean isExportConfig) throws Exception {
        codeProject.buildModules(new NullProgressMonitor(), null, null);
        if (isExportConfig) {
            Map<String, Object> argumentsMap = new HashMap<>();
            argumentsMap.put(TalendProcessArgumentConstant.ARG_GOAL, TalendMavenConstants.GOAL_INSTALL);
            argumentsMap.put(TalendProcessArgumentConstant.ARG_PROGRAM_ARGUMENTS, "-Dmaven.main.skip=true"); //$NON-NLS-1$
            codeProject.buildModules(new NullProgressMonitor(), null, argumentsMap);
        }
    }
    
    /**
     * 
     * sync the bat/sh/jobInfo to resources template folder.
     */
    public void syncTemplates(boolean overwrite) throws Exception {
        IFolder templateFolder = codeProject.getTemplatesFolder();

        IFile shFile = templateFolder.getFile(IProjectSettingTemplateConstants.JOB_RUN_SH_TEMPLATE_FILE_NAME);
        IFile batFile = templateFolder.getFile(IProjectSettingTemplateConstants.JOB_RUN_BAT_TEMPLATE_FILE_NAME);
        IFile infoFile = templateFolder.getFile(IProjectSettingTemplateConstants.JOB_INFO_TEMPLATE_FILE_NAME);

        final Map<String, Object> templateParameters = PomUtil.getTemplateParameters(codeProject.getPropery());
        String shContent = MavenTemplateManager.getProjectSettingValue(IProjectSettingPreferenceConstants.TEMPLATE_SH,
                templateParameters);
        String batContent = MavenTemplateManager.getProjectSettingValue(IProjectSettingPreferenceConstants.TEMPLATE_BAT,
                templateParameters);
        String jobInfoContent = MavenTemplateManager.getProjectSettingValue(IProjectSettingPreferenceConstants.TEMPLATE_JOB_INFO,
                templateParameters);

        MavenTemplateManager.saveContent(shFile, shContent, overwrite);
        MavenTemplateManager.saveContent(batFile, batContent, overwrite);
        MavenTemplateManager.saveContent(infoFile, jobInfoContent, overwrite);
    }

    /**
     * 
     * add the job to the pom modules list of project.
     */
    public void addChildModules(boolean removeOld, String... childModules) throws Exception {
        IFile projectPomFile = codeProject.getProjectPom();

        MavenModelManager mavenModelManager = MavenPlugin.getMavenModelManager();
        Model projModel = mavenModelManager.readMavenModel(projectPomFile);
        List<String> modules = projModel.getModules();
        if (modules == null) {
            modules = new ArrayList<String>();
            projModel.setModules(modules);
        }

        boolean modifed = false;
        if (removeOld || childModules == null || childModules.length == 0) { // clean the modules
            if (!modules.isEmpty()) {
                modules.clear();
                modifed = true;
            }
        }

        final Iterator<String> iterator = modules.iterator();
        while (iterator.hasNext()) {
            String module = iterator.next();
            if (ArrayUtils.contains(childModules, module)) {
                iterator.remove(); // remove the exised one
            }
        }

        if (childModules != null) {
            // according to the arrays order to add the modules.
            for (String module : childModules) {
                if (module.length() > 0) {
                    modules.add(module);
                    modifed = true;
                }
            }
        }

        if (modifed) {
            // save pom.
            PomUtil.savePom(null, projModel, projectPomFile);
        }
    }

    /**
     * 
     * Clean the pom_xxx.xml and assembly_xxx.xml and target folder, also clean the module and dependencies.
     * 
     * another cleaning up for sources codes or such in @see DeleteAllJobWhenStartUp.
     */
    public void cleanMavenFiles(IProgressMonitor monitor) throws Exception {
        IJavaProject jProject = codeProject.getJavaProject();
        if (!jProject.isOpen()) {
            jProject.open(monitor);
        }
        // empty the src/main/java...
        IFolder srcFolder = codeProject.getSrcFolder();
        codeProject.cleanFolder(monitor, srcFolder);
        // contexts
        IFolder resourcesFolder = codeProject.getResourcesFolder();
        emptyContexts(monitor, resourcesFolder, codeProject);

        // empty the outputs, target
        IFolder targetFolder = codeProject.getTargetFolder();
        codeProject.cleanFolder(monitor, targetFolder);

        // empty the src/test/java
        IFolder testSrcFolder = codeProject.getTestSrcFolder();
        codeProject.cleanFolder(monitor, testSrcFolder);

        // empty the src/test/java (main for contexts)
        IFolder testResourcesFolder = codeProject.getTestResourcesFolder();
        codeProject.cleanFolder(monitor, testResourcesFolder);

        // rules
        IFolder rulesResFolder = codeProject.getResourceSubFolder(monitor, JavaUtils.JAVA_RULES_DIRECTORY);
        codeProject.cleanFolder(monitor, rulesResFolder);

        // sqltemplate
        IFolder sqlTemplateResFolder = codeProject.getResourceSubFolder(monitor, JavaUtils.JAVA_SQLPATTERNS_DIRECTORY);
        codeProject.cleanFolder(monitor, sqlTemplateResFolder);
        
        // clean all assemblies in src/main/assemblies
        fullCleanupContainer(codeProject.getAssembliesFolder());

        // clean all items in src/main/items
        fullCleanupContainer(codeProject.getItemsFolder());

        // clean all items in tests
        fullCleanupContainer(codeProject.getTestsFolder());

        codeProject.getProject().refreshLocal(IResource.DEPTH_ONE, monitor);

    }

    private void fullCleanupContainer(IContainer container) {
        if (container != null && container.exists()) {
            FilesUtils.deleteFolder(container.getLocation().toFile(), false);
        }
    }

    private void cleanupContainer(IContainer container, FilenameFilter filter) {
        File folder = container.getLocation().toFile();
        if (filter != null) {
            deleteFiles(folder.listFiles(filter));
        } else {
            fullCleanupContainer(container);
        }
    }

    private void deleteFiles(File[] files) {
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    f.delete();
                }
            }
        }
    }

    public void syncCodesPoms(IProgressMonitor monitor, IProcessor processor, boolean overwrite) throws Exception {
        final IProcess process = processor != null ? processor.getProcess() : null;

        syncRoutinesPom(processor.getProperty(), overwrite);
        // PigUDFs
        if (ProcessUtils.isRequiredPigUDFs(process)) {
            syncPigUDFsPom(processor.getProperty(), overwrite);
        }
        // Beans
        if (ProcessUtils.isRequiredBeans(process)) {
            syncBeansPom(processor.getProperty(), overwrite);
        }
    }

    private static void emptyContexts(IProgressMonitor monitor, IContainer baseContainer,
            ITalendProcessJavaProject talendJavaProject) throws CoreException {
        IPath location = baseContainer.getLocation();
        File[] listFiles = location.toFile().listFiles();
        if (listFiles != null) { // for each project sub folder
            for (File child : listFiles) {
                File testContextFile = findTestContextFile(child);
                if (testContextFile != null) {
                    IPath testContextPath = new Path(testContextFile.toString());
                    IPath testRelativePath = testContextPath.makeRelativeTo(location);
                    String projectSegment = testRelativePath.segment(0);
                    IFolder folder = baseContainer.getFolder(new Path(projectSegment));
                    talendJavaProject.cleanFolder(monitor, folder);
                    if (folder.exists()) { // also delete it
                        folder.delete(true, monitor);
                    }
                }
            }
        }
    }

    private static File findTestContextFile(File file) {
        if (file != null) {
            if (file.getName().endsWith(JavaUtils.JAVA_CONTEXT_EXTENSION)
                    && file.getParentFile().getName().equals(JavaUtils.JAVA_CONTEXTS_DIRECTORY)) {
                return file;
            }
            if (file.isDirectory()) {
                File[] listFiles = file.listFiles();
                if (listFiles != null) {
                    for (File f : listFiles) {
                        File contextFile = findTestContextFile(f);
                        if (contextFile != null) {
                            return contextFile;
                        }
                    }
                }
            }
        }
        return null;
    }
    
}
