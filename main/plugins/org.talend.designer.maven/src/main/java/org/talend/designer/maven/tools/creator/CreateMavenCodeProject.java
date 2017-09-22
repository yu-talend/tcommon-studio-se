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
package org.talend.designer.maven.tools.creator;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.internal.Messages;
import org.eclipse.m2e.core.internal.project.ProjectConfigurationManager;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.general.TalendJobNature;
import org.talend.core.runtime.projectsetting.IProjectSettingTemplateConstants;
import org.talend.designer.maven.DesignerMavenPlugin;
import org.talend.designer.maven.model.MavenSystemFolders;
import org.talend.designer.maven.model.ProjectSystemFolder;
import org.talend.designer.maven.model.TalendMavenConstants;
import org.talend.designer.maven.template.MavenTemplateManager;
import org.talend.designer.maven.utils.PomIdsHelper;
import org.talend.designer.maven.utils.PomUtil;

/**
 * created by ggu on 22 Jan 2015 Detailled comment
 *
 */
public class CreateMavenCodeProject extends AbstractMavenGeneralTemplatePom {

    public static final String IS_ALREADY_SET_ECLIPSE_COMPLIANCE = "IS_ALREADY_SET_ECLIPSE_COMPLIANCE"; //$NON-NLS-1$

    private IProject project;

    private IFile pomFile;
    
    private IPath location;

    public CreateMavenCodeProject(IProject project) {
        super(project.getFile(TalendMavenConstants.POM_FILE_NAME), IProjectSettingTemplateConstants.PROJECT_TEMPLATE_FILE_NAME);
        Assert.isNotNull(project);
        this.project = project;
    }

    public IProject getProject() {
        return this.project;
    }

    @Override
    protected Model createModel() {
        // temp model.
        Model templateModel = new Model();
        templateModel.setModelVersion("4.0.0"); //$NON-NLS-1$
        templateModel.setGroupId(PomIdsHelper.getJobGroupId(project.getName()));
        templateModel.setArtifactId("talend"); //$NON-NLS-1$
        templateModel.setVersion(PomIdsHelper.getProjectVersion());
        templateModel.setPackaging(TalendMavenConstants.PACKAGING_JAR);
        return templateModel;
    }

    /**
     * 
     * By default, it's current workspace.
     * 
     */
    protected IPath getBaseLocation() {
        return ResourcesPlugin.getWorkspace().getRoot().getLocation();
    }

    public void setProjectLocation(IPath location) {
        this.location = location;
    }
    
    public void setPomFile(IFile pomFile) {
        this.pomFile = pomFile;
    }

    /**
     * 
     * By default, create the all maven folders.
     * 
     */
    protected String[] getFolders() {
        ProjectSystemFolder[] mavenDirectories = MavenSystemFolders.ALL_DIRS;

        String[] directories = new String[mavenDirectories.length];
        for (int i = 0; i < directories.length; i++) {
            directories[i] = mavenDirectories[i].getPath();
        }

        return directories;
    }

    /**
     * 
     * can do something before create operation.
     */
    protected void beforeCreate(IProgressMonitor monitor, IResource res) throws Exception {
        cleanLastUpdatedFiles();
    }

    /**
     * 
     * after create operation, can do something, like add some natures.
     */
    protected void afterCreate(IProgressMonitor monitor, IResource res) throws Exception {
        IProject p = res.getProject();
        if (!p.isOpen()) {
            p.open(monitor);
        }
        addTalendNature(p, TalendJobNature.ID, monitor);
        //convertJavaProjectToPom(monitor, p);
        PomUtil.addToParentModules(pomFile);
        changeClasspath(monitor, p);

        IJavaProject javaProject = JavaCore.create(p);
        clearProjectIndenpendComplianceSettings(javaProject);
        // unregist listeners, release resources
        javaProject.close();
    }

    @Override
    public void create(IProgressMonitor monitor) throws Exception {
        IProgressMonitor pMoniter = monitor;
        if (monitor == null) {
            pMoniter = new NullProgressMonitor();
        }
        IProgressMonitor subMonitor = new SubProgressMonitor(pMoniter, 100);
        
        Model model;
//        if (pomFile.exists()) {
//            model = MavenPlugin.getMavenModelManager().readMavenModel(pomFile);
//        } else {
//            // first time create, use temp model.
//        }
        // always use temp model to avoid classpath problem?
        model = createModel();

        final ProjectImportConfiguration importConfiguration = new ProjectImportConfiguration();
        IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
        beforeCreate(subMonitor, p);

        subMonitor.worked(10);

        createSimpleProject(subMonitor, p, model, importConfiguration);
        // MavenPlugin.getProjectConversionManager().convert(project, model, subMonitor);

        subMonitor.worked(80);
        
        afterCreate(subMonitor, p);

        subMonitor.done();

        project = p;
    }

    public static void addTalendNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
        if (!project.hasNature(natureId)) {
            IProjectDescription description = project.getDescription();
            String[] prevNatures = description.getNatureIds();
            String[] newNatures = new String[prevNatures.length + 1];
            System.arraycopy(prevNatures, 0, newNatures, 1, prevNatures.length);
            newNatures[0] = natureId;
            description.setNatureIds(newNatures);
            project.setDescription(description, monitor);
        }
    }

    //TODO remove?
    private void convertJavaProjectToPom(IProgressMonitor monitor, IProject p) {
        IFile pomFile = p.getFile(TalendMavenConstants.POM_FILE_NAME);
        if (pomFile.exists()) {
            try {
                MavenModelManager mavenModelManager = MavenPlugin.getMavenModelManager();
                MavenProject mavenProject = mavenModelManager.readMavenProject(pomFile, monitor);
                if (mavenProject != null) {
                    Model model = mavenProject.getOriginalModel();
                    // if not pom, change to pom
                    if (!TalendMavenConstants.PACKAGING_POM.equals(model.getPackaging())) {

                        Model codeProjectTemplateModel = MavenTemplateManager.getCodeProjectTemplateModel();
                        model.setGroupId(codeProjectTemplateModel.getGroupId());
                        model.setArtifactId(codeProjectTemplateModel.getArtifactId());
                        model.setVersion(codeProjectTemplateModel.getVersion());
                        model.setName(codeProjectTemplateModel.getName());
                        model.setPackaging(codeProjectTemplateModel.getPackaging());

                        PomUtil.savePom(monitor, model, pomFile);

                        p.refreshLocal(IResource.DEPTH_ONE, monitor);
                    }
                }
            } catch (Exception e) {
                ExceptionHandler.process(e);
            }

        }
    }

    public static void changeClasspath(IProgressMonitor monitor, IProject p) {
        try {
            IJavaProject javaProject = JavaCore.create(p);
            IClasspathEntry[] rawClasspathEntries = javaProject.getRawClasspath();
            boolean changed = false;
            boolean foundResources = false;

            for (int index = 0; index < rawClasspathEntries.length; index++) {
                IClasspathEntry entry = rawClasspathEntries[index];

                IClasspathEntry newEntry = null;
                if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath path = entry.getPath();
                    if (p.getFullPath().isPrefixOf(path)) {
                        path = path.removeFirstSegments(1);
                    }

                    // src/main/resources, in order to removing the 'excluding="**"'.
                    if (MavenSystemFolders.RESOURCES.getPath().equals(path.toString())) {
                        foundResources = true;
                        newEntry = JavaCore.newSourceEntry(entry.getPath(), new IPath[0], new IPath[0], //
                                entry.getOutputLocation(), entry.getExtraAttributes());
                    }

                    // src/test/resources, in order to removing the 'excluding="**"'.
                    if (MavenSystemFolders.RESOURCES_TEST.getPath().equals(path.toString())) {
                        newEntry = JavaCore.newSourceEntry(entry.getPath(), new IPath[0], new IPath[0], //
                                entry.getOutputLocation(), entry.getExtraAttributes());
                    }

                } else if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                    // remove the special version of jre in container.
                    IPath defaultJREContainerPath = JavaRuntime.newDefaultJREContainerPath();
                    if (defaultJREContainerPath.isPrefixOf(entry.getPath())) {
                        // JavaRuntime.getDefaultJREContainerEntry(); //missing properties
                        newEntry = JavaCore.newContainerEntry(defaultJREContainerPath, entry.getAccessRules(),
                                entry.getExtraAttributes(), entry.isExported());
                    }
                }
                if (newEntry != null) {
                    rawClasspathEntries[index] = newEntry;
                    changed = true;
                }

            }
            if (!foundResources) {
                List<IClasspathEntry> list = new LinkedList<>(Arrays.asList(rawClasspathEntries));
                IFolder resources = p.getFolder("src/main/resources");
                IFolder output = p.getFolder("target/classes");
                ClasspathAttribute attribute = new ClasspathAttribute("maven.pomderived", Boolean.TRUE.toString());
                IClasspathEntry newEntry = JavaCore.newSourceEntry(resources.getFullPath(), new IPath[0], new IPath[0],
                        output.getFullPath(), new IClasspathAttribute[]{attribute});
                list.add(1, newEntry);
                rawClasspathEntries = list.toArray(new IClasspathEntry[]{});
                changed = true;
            }
            if (changed) {
                javaProject.setRawClasspath(rawClasspathEntries, monitor);
            }
        } catch (CoreException e) {
            ExceptionHandler.process(e);
        }
    }

    /**
     * 
     * FIXME, Maybe need find another way to remove the lastUpdated files. seems the
     * MavenUpdateRequest.isForceDependencyUpdate is not useful when create this .Java project.
     */
    private void cleanLastUpdatedFiles() {
        final IMaven maven = MavenPlugin.getMaven();
        String localRepositoryPath = maven.getLocalRepositoryPath();
        if (localRepositoryPath == null) {
            return;
        }
        File localRepoFolder = new File(localRepositoryPath);
        cleanLastUpdatedFile(localRepoFolder);
    }

    private final static FileFilter lastUpdatedFilter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return pathname.isDirectory() || pathname.getName().endsWith(".lastUpdated") //$NON-NLS-1$
                    || pathname.getName().equals("m2e-lastUpdated.properties"); //$NON-NLS-1$
        }
    };

    private void cleanLastUpdatedFile(final File file) {
        if (file != null && file.exists()) {
            if (file.isDirectory()) {
                File[] list = file.listFiles(lastUpdatedFilter);
                if (list != null) {
                    for (File f : list) {
                        cleanLastUpdatedFile(f);
                    }
                }
            } else if (file.isFile() && lastUpdatedFilter.accept(file)) {
                file.delete();
            }
        }
    }

    /**
     * Clear compliance settings from project, and set them into Eclipse compliance settings
     * 
     * @param javaProject
     */
    private static void clearProjectIndenpendComplianceSettings(IJavaProject javaProject) {

        Map<String, String> projectComplianceOptions = javaProject.getOptions(false);
        if (projectComplianceOptions == null || projectComplianceOptions.isEmpty()) {
            return;
        }
        String compilerCompliance = javaProject.getOption(JavaCore.COMPILER_COMPLIANCE, false);
        // clear compliance settings from project
        Set<String> keySet = projectComplianceOptions.keySet();
        for (String key : keySet) {
            javaProject.setOption(key, null);
        }

        IEclipsePreferences pluginPreferences = InstanceScope.INSTANCE.getNode(DesignerMavenPlugin.PLUGIN_ID);
        boolean isAlreadySetEclipsePreferences = pluginPreferences.getBoolean(IS_ALREADY_SET_ECLIPSE_COMPLIANCE, false);
        // if already setted them, then can't modify them anymore since user can customize them.
        if (!isAlreadySetEclipsePreferences) {
            pluginPreferences.putBoolean(IS_ALREADY_SET_ECLIPSE_COMPLIANCE, true);
            if (compilerCompliance != null) {
                IEclipsePreferences eclipsePreferences = InstanceScope.INSTANCE.getNode(JavaCore.PLUGIN_ID);
                // set compliance settings to Eclipse
                Map<String, String> complianceOptions = new HashMap<String, String>();
                JavaCore.setComplianceOptions(compilerCompliance, complianceOptions);
                if (!complianceOptions.isEmpty()) {
                    Set<Entry<String, String>> entrySet = complianceOptions.entrySet();
                    for (Entry<String, String> entry : entrySet) {
                        eclipsePreferences.put(entry.getKey(), entry.getValue());
                    }
                }
                try {
                    // save changes
                    eclipsePreferences.flush();
                    pluginPreferences.flush();
                } catch (BackingStoreException e) {
                    ExceptionHandler.process(e);
                }
            }
        }
    }

    @SuppressWarnings("restriction")
    private void createSimpleProject(IProgressMonitor monitor, IProject p, Model model, ProjectImportConfiguration importConfiguration) throws CoreException {
        final String[] directories = getFolders();

        ProjectConfigurationManager projectConfigurationManager = (ProjectConfigurationManager) MavenPlugin
                .getProjectConfigurationManager();

        String projectName = p.getName();
        monitor.beginTask(NLS.bind(Messages.ProjectConfigurationManager_task_creating, projectName), 5);

        monitor.subTask(Messages.ProjectConfigurationManager_task_creating_workspace);
        IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(projectName);
        description.setLocation(location);
        p.create(description, monitor);
        p.open(monitor);
        monitor.worked(1);

        hideNestedProjectsFromParents(Collections.singletonList(p));

        monitor.worked(1);
        
        monitor.subTask(Messages.ProjectConfigurationManager_task_creating_pom);
        IFile pomFile = p.getFile(TalendMavenConstants.POM_FILE_NAME);
        if (!pomFile.exists()) {
            // TODO surround by workunit.
            MavenPlugin.getMavenModelManager().createMavenModel(pomFile, model);
        }
        monitor.worked(1);

        monitor.subTask(Messages.ProjectConfigurationManager_task_creating_folders);
        for (int i = 0; i < directories.length; i++) {
            ProjectConfigurationManager.createFolder(p.getFolder(directories[i]), false);
        }
        monitor.worked(1);

        monitor.subTask(Messages.ProjectConfigurationManager_task_creating_project);
        projectConfigurationManager.enableMavenNature(p, importConfiguration.getResolverConfiguration(), monitor);
        monitor.worked(1);
        
        if (this.pomFile == null) {
            this.pomFile = pomFile;
        }
    }

    private void hideNestedProjectsFromParents(List<IProject> projects) {

        if (!MavenPlugin.getMavenConfiguration().isHideFoldersOfNestedProjects()) {
            return;
        }

        // Prevent child project folders from showing up in parent project folders.

        HashMap<File, IProject> projectFileMap = new HashMap<File, IProject>();

        for (IProject project : projects) {
            projectFileMap.put(project.getLocation().toFile(), project);
        }
        for (IProject project : projects) {
            File projectFile = project.getLocation().toFile();
            IProject physicalParentProject = projectFileMap.get(projectFile.getParentFile());
            if (physicalParentProject == null) {
                continue;
            }
            IFolder folder = physicalParentProject.getFolder(projectFile.getName());
            if (folder.exists()) {
                try {
                    folder.setHidden(true);
                } catch (Exception ex) {
                    // log.error("Failed to hide resource; " + resource.getLocation().toOSString(), ex);
                    ExceptionHandler.process(ex);
                }
            }
        }
    }

}
