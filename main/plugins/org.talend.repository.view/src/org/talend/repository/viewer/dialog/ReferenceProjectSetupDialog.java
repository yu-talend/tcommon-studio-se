package org.talend.repository.viewer.dialog;

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

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.CorePlugin;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.PluginChecker;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.model.properties.ProjectReference;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.services.IGITProviderService;
import org.talend.core.services.ISVNProviderService;
import org.talend.repository.ProjectManager;
import org.talend.repository.i18n.Messages;
import org.talend.repository.localprovider.model.BaseReferenceProjectProvider;
import org.talend.repository.model.IRepositoryService;
import org.talend.repository.ui.dialog.OverTimePopupDialogTask;

public class ReferenceProjectSetupDialog extends TitleAreaDialog {

    private static final int REPOSITORY_LOCAL = 0;

    private static final int REPOSITORY_GIT = 1;

    private static final int REPOSITORY_SVN = 2;

    private ListViewer viewer;

    private Combo projectCombo;

    private Combo branchCombo;

    private Project[] projects;

    private Project lastSelectedProject;

    private ISVNProviderService svnProviderService;

    private IGITProviderService gitProviderService;

    private List<ProjectReferenceBean> viewerInput = new ArrayList<ProjectReferenceBean>();

    private boolean isModified = false;

    public ReferenceProjectSetupDialog(Shell parentShell) {
        super(parentShell);
        if (PluginChecker.isSVNProviderPluginLoaded()) {
            try {
                svnProviderService = (ISVNProviderService) GlobalServiceRegister.getDefault()
                        .getService(ISVNProviderService.class);
                gitProviderService = (IGITProviderService) GlobalServiceRegister.getDefault()
                        .getService(IGITProviderService.class);
            } catch (RuntimeException e) {
                // nothing to do
            }
        }
    }

    protected Control createDialogArea(final Composite parent) {
        Composite composite = new Composite(parent, SWT.None);
        composite.setLayout(new GridLayout(2, false));
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);

        SashForm form = new SashForm(composite, SWT.HORIZONTAL);
        form.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group listGroup = new Group(form, SWT.None);
        listGroup.setLayout(new GridLayout(2, false));
        listGroup.setText(Messages.getString("ReferenceProjectSetupDialog.ListGroup"));//$NON-NLS-1$

        Group addGroup = new Group(form, SWT.None);
        addGroup.setLayout(new GridLayout(2, false));
        addGroup.setText(Messages.getString("ReferenceProjectSetupDialog.AddGroup"));//$NON-NLS-1$

        viewer = new ListViewer(listGroup, SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setLabelProvider(new ReferenceProjectLabelProvider());
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        GridData viewerData = new GridData(GridData.FILL_BOTH);
        viewerData.horizontalSpan = 2;
        viewer.getControl().setLayoutData(viewerData);

        Button removeButton = new Button(listGroup, SWT.None);
        removeButton.setText(Messages.getString("ReferenceProjectSetupDialog.ButtonDelete")); //$NON-NLS-1$
        GridData removeButtonData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        removeButtonData.horizontalSpan = 2;
        removeButton.setLayoutData(removeButtonData);
        removeButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                removeProjectReference();
            }
        });

        Label projectLabel = new Label(addGroup, SWT.None);
        projectLabel.setLayoutData(new GridData());
        projectLabel.setText(Messages.getString("ReferenceProjectSetupDialog.LabelProject"));//$NON-NLS-1$

        projectCombo = new Combo(addGroup, SWT.BORDER);
        projectCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        projectCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setErrorMessage(null);
                Project project = getCurrentSelectedProject();
                if (lastSelectedProject == null
                        || (project != null && !project.getTechnicalLabel().equals(lastSelectedProject.getTechnicalLabel()))) {
                    lastSelectedProject = project;
                    initBranchData();
                }
            }
        });

        Label branchLabel = new Label(addGroup, SWT.None);
        branchLabel.setLayoutData(new GridData());
        branchLabel.setText(Messages.getString("ReferenceProjectSetupDialog.LabelBranch"));//$NON-NLS-1$

        branchCombo = new Combo(addGroup, SWT.BORDER);
        branchCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        branchCombo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setErrorMessage(null);
            }
        });

        Button addButton = new Button(addGroup, SWT.None);
        GridData addData = new GridData(GridData.HORIZONTAL_ALIGN_END);
        addData.horizontalSpan = 2;
        addButton.setLayoutData(addData);
        addButton.setText(Messages.getString("ReferenceProjectSetupDialog.ButtonAdd")); //$NON-NLS-1$
        addButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                addProjectReference();
            }
        });

        form.setWeights(new int[] { 1, 1 });
        this.setTitle(Messages.getString("ReferenceProjectSetupDialog.Title")); //$NON-NLS-1$

        applyDialogFont(composite);
        initViewerData();
        initProjectData();
        return composite;
    }

    private void initViewerData() {
        Project project = ProjectManager.getInstance().getCurrentProject();
        viewerInput.addAll(getReferenceProjectData(project));
        viewer.setInput(viewerInput);
        viewer.refresh();
    }

    private List<ProjectReferenceBean> getReferenceProjectData(Project project) {
        List<ProjectReferenceBean> result = new ArrayList<ProjectReferenceBean>();
        @SuppressWarnings("unchecked")
        List<ProjectReference> list = project.getEmfProject().getReferencedProjects();
        for (ProjectReference pr : list) {
            ProjectReferenceBean prb = new ProjectReferenceBean();
            prb.setReferenceProject(pr.getReferencedProject());
            prb.setReferenceBranch(pr.getReferencedBranch());
            result.add(prb);
        }
        return result;
    }

    private void initProjectData() {
        this.setErrorMessage(null);
        projectCombo.setEnabled(false);
        projectCombo.setItems(new String[0]);
        String errorMessage = null;

        OverTimePopupDialogTask<Project[]> overTimePopupDialogTask = new OverTimePopupDialogTask<Project[]>() {

            @Override
            public Project[] run() throws Throwable {
                return ProxyRepositoryFactory.getInstance().readProject();
            }
        };
        overTimePopupDialogTask.setNeedWaitingProgressJob(false);
        try {
            projects = overTimePopupDialogTask.runTask();
        } catch (Throwable e) {
            errorMessage = e.getLocalizedMessage();
        }
        if (projects != null && projects.length > 0) {
            Project currentProject = ProjectManager.getInstance().getCurrentProject();
            int projectRepositoryType = getProjectRepositoryType(currentProject);
            List<String> itemList = new ArrayList<String>();
            for (int i = 0; i < projects.length; i++) {
                if (currentProject.getTechnicalLabel().equals(projects[i].getTechnicalLabel())) {
                    continue;
                }
                if (projectRepositoryType == getProjectRepositoryType(projects[i])) {
                    itemList.add(projects[i].getTechnicalLabel());
                }
            }
            projectCombo.setItems(itemList.toArray(new String[0]));
        }
        projectCombo.setEnabled(true);
        this.setErrorMessage(errorMessage);
    }

    /**
     * Get project repository type
     * 
     * @param project
     */
    private int getProjectRepositoryType(Project project) {
        try {
            if (!project.isLocal()) {
                if (gitProviderService != null && gitProviderService.isGITProject(project)) {
                    return REPOSITORY_GIT;
                }
            }
            if (svnProviderService != null && svnProviderService.isSVNProject(project)) {
                return REPOSITORY_SVN;
            }
        } catch (PersistenceException ex) {
            ExceptionHandler.process(ex);
        }

        return REPOSITORY_LOCAL;
    }

    private void initBranchData() {
        this.setErrorMessage(null);
        branchCombo.setEnabled(false);
        branchCombo.setItems(new String[0]);
        String errorMessage = null;
        List<String> allBranch;

        Project currentProject = ProjectManager.getInstance().getCurrentProject();
        int projectRepositoryType = getProjectRepositoryType(currentProject);
        if (REPOSITORY_LOCAL == projectRepositoryType) {
            return;
        }
        if (projectRepositoryType == REPOSITORY_SVN && this.getRepositoryContext().isOffline()) {
            this.setErrorMessage(Messages.getString("ReferenceProjectSetupDialog.ErrorCanNotGetSVNBranchData")); //$NON-NLS-1$
            return;
        }
        OverTimePopupDialogTask<List<String>> overTimePopupDialogTask = new OverTimePopupDialogTask<List<String>>() {

            @Override
            public List<String> run() throws Throwable {
                IRepositoryService repositoryService = (IRepositoryService) GlobalServiceRegister.getDefault()
                        .getService(IRepositoryService.class);
                if (repositoryService != null) {
                    return repositoryService.getProjectBranch(lastSelectedProject);
                }
                return null;
            }
        };
        overTimePopupDialogTask.setNeedWaitingProgressJob(false);
        try {
            allBranch = overTimePopupDialogTask.runTask();
            if (allBranch != null) {
                branchCombo.setItems(allBranch.toArray(new String[0]));
            }
            if (projectRepositoryType == REPOSITORY_SVN) {
                if (!allBranch.contains("trunk")) {//$NON-NLS-1$
                    allBranch.add("trunk");//$NON-NLS-1$
                }
                branchCombo.setItems(allBranch.toArray(new String[0]));
                branchCombo.setText("trunk");//$NON-NLS-1$
            } else if (projectRepositoryType == REPOSITORY_GIT) {
                branchCombo.setItems(allBranch.toArray(new String[0]));
                branchCombo.setText("master");//$NON-NLS-1$
            }
        } catch (Throwable e) {
            errorMessage = e.getLocalizedMessage();
        }
        branchCombo.setEnabled(true);
        this.setErrorMessage(errorMessage);
    }

    private RepositoryContext getRepositoryContext() {
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext()
                .getProperty(Context.REPOSITORY_CONTEXT_KEY);
        return repositoryContext;
    }

    protected Project getCurrentSelectedProject() {
        String label = projectCombo.getText();
        for (Project project : projects) {
            if (label.equals(project.getTechnicalLabel())) {
                return project;
            }
        }
        return null;
    }

    private void addProjectReference() {
        this.setErrorMessage(null);
        Project p = getCurrentSelectedProject();
        if (p != null) {
            int projectRepositoryType = this.getProjectRepositoryType(p);
            String branch = "";
            if (REPOSITORY_LOCAL != projectRepositoryType) {
                branch = branchCombo.getText();
                if (branch.length() == 0) {
                    this.setErrorMessage(Messages.getString("ReferenceProjectSetupDialog.ErrorBranchEmpty"));//$NON-NLS-1$
                    return;
                }
            }
            for (ProjectReferenceBean bean : viewerInput) {
                if (bean.getReferenceProject().getTechnicalLabel().equals(p.getTechnicalLabel())) {
                    this.setErrorMessage(Messages.getString("ReferenceProjectSetupDialog.ErrorContainedProject"));//$NON-NLS-1$
                    return;
                }
            }
            for (ProjectReferenceBean bean : viewerInput) {
                List<ProjectReference> referencedProjectList = getAllReferenceProject(bean.getReferenceProject());
                if (referencedProjectList != null && referencedProjectList.size() > 0) {
                    for (ProjectReference pr : referencedProjectList) {
                        if (pr.getReferencedProject().getTechnicalLabel().equals(p.getTechnicalLabel())
                                && !branch.equals(pr.getReferencedBranch())) {
                            this.setErrorMessage(Messages.getString("ReferenceProjectSetupDialog.ErrorReferencedByOtherProject",
                                    getProjectDecription(p.getLabel(), pr.getReferencedBranch()), pr.getProject().getLabel()));// $NON-NLS-1$
                            return;
                        }
                    }
                }
            }
            ProjectReferenceBean bean = new ProjectReferenceBean();
            bean.setReferenceProject(p.getEmfProject());
            bean.setReferenceBranch(branch);
            viewerInput.add(bean);
            viewer.refresh();
        }
    }

    @SuppressWarnings("unchecked")
    private List<ProjectReference> getAllReferenceProject(org.talend.core.model.properties.Project project) {
        List<ProjectReference> result = new ArrayList<ProjectReference>();
        List<ProjectReference> referenceList = project.getReferencedProjects();
        if (referenceList != null && referenceList.size() > 0) {
            for (ProjectReference reference : referenceList) {
                if (reference.getReferencedProject().getReferencedProjects() != null) {
                    result.add(reference);
                    result.addAll(getAllReferenceProject(reference.getReferencedProject()));
                }
            }
        }

        return result;
    }

    private void removeProjectReference() {
        ISelection selection = viewer.getSelection();
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            viewerInput.removeAll(((IStructuredSelection) selection).toList());
        }
        viewer.refresh();
    }

    protected static String getProjectDecription(String projectName, String branch) {
        StringBuffer sb = new StringBuffer();
        sb.append(projectName);
        if (branch != null && branch.trim().length() > 0) {
            sb.append("/").append(branch);
        }
        return sb.toString();
    }

    protected void okPressed() {
        List<ProjectReferenceBean> originList = getReferenceProjectData(ProjectManager.getInstance().getCurrentProject());
        if (originList.size() != viewerInput.size()) {
            isModified = true;
        }
        if (!isModified) {
            for (ProjectReferenceBean originValue : originList) {
                boolean isFind = false;
                for (ProjectReferenceBean value : viewerInput) {
                    if (originValue.getReferenceProject().getTechnicalLabel()
                            .equals(value.getReferenceProject().getTechnicalLabel())
                            && originValue.getReferenceBranch().equals(value.getReferenceBranch())) {
                        isFind = true;
                        break;
                    }
                }
                if (!isFind) {
                    isModified = true;
                    break;
                }
            }
        }
        if (isModified) {
            List<ProjectReference> projectReferenceList = new ArrayList<ProjectReference>();
            for (ProjectReferenceBean bean : viewerInput) {
                ProjectReference pr = PropertiesFactory.eINSTANCE.createProjectReference();
                pr.setReferencedBranch(bean.getReferenceBranch());
                pr.setReferencedProject(bean.getReferenceProject());
                projectReferenceList.add(pr);
            }
            BaseReferenceProjectProvider referenceProjectProvider = new BaseReferenceProjectProvider(
                    ProjectManager.getInstance().getCurrentProject().getEmfProject());
            referenceProjectProvider.setProjectReference(projectReferenceList);
            String errorMessages = null;
            try {
                referenceProjectProvider.saveSettings();
            } catch (Exception e) {
                errorMessages = e.getMessage();
                this.setErrorMessage(errorMessages);
            }
            if (errorMessages != null) {
                return;
            }
        }
        super.okPressed();
    }

    public boolean isModified() {
        return isModified;
    }

    /**
     * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
     */
    protected void configureShell(final Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.getString("ReferenceProjectSetupDialog.Title")); //$NON-NLS-1$
        newShell.setSize(600, 450);
    }

    protected int getShellStyle() {
        return SWT.RESIZE | SWT.MAX | SWT.CLOSE;
    }

    @Override
    protected void initializeBounds() {
        super.initializeBounds();

        Point size = getShell().getSize();
        Point location = getInitialLocation(size);
        getShell().setBounds(getConstrainedShellBounds(new Rectangle(location.x, location.y, size.x, size.y)));
    }

}

class ReferenceProjectLabelProvider implements ILabelProvider {

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        return null;
    }

    @Override
    public String getText(Object element) {
        if (element instanceof ProjectReferenceBean) {
            ProjectReferenceBean pr = (ProjectReferenceBean) element;
            StringBuffer sb = new StringBuffer();
            sb.append(ReferenceProjectSetupDialog.getProjectDecription(pr.getReferenceProject().getTechnicalLabel(),
                    pr.getReferenceBranch()));
            return sb.toString();
        }
        return null;
    }
}

class ProjectReferenceBean {

    private String referenceBranch;

    private org.talend.core.model.properties.Project referenceProject;

    public String getReferenceBranch() {
        return referenceBranch;
    }

    public void setReferenceBranch(String referenceBranch) {
        this.referenceBranch = referenceBranch;
    }

    public org.talend.core.model.properties.Project getReferenceProject() {
        return referenceProject;
    }

    public void setReferenceProject(org.talend.core.model.properties.Project referenceProject) {
        this.referenceProject = referenceProject;
    }
}
