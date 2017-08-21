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
package org.talend.repository.viewer.action;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.model.general.Project;
import org.talend.core.model.utils.RepositoryManagerHelper;
import org.talend.core.repository.model.ProjectRepositoryNode;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryViewPlugin;
import org.talend.repository.i18n.Messages;
import org.talend.repository.navigator.RepoViewCommonNavigator;
import org.talend.repository.ui.views.IRepositoryView;
import org.talend.repository.viewer.dialog.ReferenceProjectSetupDialog;

public class RepoReferenceProjectSetupAction implements IViewActionDelegate {

    RepoViewCommonNavigator view;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        ReferenceProjectSetupDialog dialog = new ReferenceProjectSetupDialog(Display.getDefault().getActiveShell());
        if (dialog.open() == Window.OK && dialog.isModified()) {
            MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
                    Messages.getString("RepoReferenceProjectSetupAction.TitleReferenceChanged"), //$NON-NLS-1$
                    Messages.getString("RepoReferenceProjectSetupAction.MsgReferenceChanged")); //$NON-NLS-1$

            IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {

                @Override
                public void run(IProgressMonitor monitor) throws CoreException {
                    Project currentProject = ProjectManager.getInstance().getCurrentProject();
                    monitor.beginTask(Messages.getString("RepoReferenceProjectSetupAction.TaskRelogin"), 10); //$NON-NLS-1$
                    monitor.subTask(Messages.getString("RepoReferenceProjectSetupAction.TaskLogoff")); //$NON-NLS-1$
                    ProxyRepositoryFactory.getInstance().logOffProject();
                    monitor.worked(2);
                    Project[] projects;
                    Project switchProject = null;
                    try {
                        projects = ProxyRepositoryFactory.getInstance().readProject();
                        for (Project p : projects) {
                            if (p.getTechnicalLabel().equals(currentProject.getTechnicalLabel())) {
                                switchProject = p;
                                break;
                            }
                        }
                        monitor.subTask(
                                Messages.getString("RepoReferenceProjectSetupAction.TaskLogon", switchProject.getLabel())); //$NON-NLS-1$
                        ProxyRepositoryFactory.getInstance().logOnProject(switchProject, monitor);
                        monitor.worked(7);
                        refreshNavigatorView();
                        monitor.worked(1);
                        monitor.done();
                    } catch (Exception e) {
                        throw new CoreException(new Status(Status.ERROR, RepositoryViewPlugin.PLUGIN_ID, e.getMessage(), e));
                    }
                }
            };

            IRunnableWithProgress iRunnableWithProgress = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                        ISchedulingRule schedulingRule = workspace.getRoot();
                        workspace.run(workspaceRunnable, schedulingRule, IWorkspace.AVOID_UPDATE, monitor);
                    } catch (CoreException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            };
            ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
            try {
                progressDialog.run(true, false, iRunnableWithProgress);
            } catch (InvocationTargetException | InterruptedException e) {
                ExceptionHandler.process(e);
            }
        }
    }

    private void refreshNavigatorView() {
        Display.getDefault().syncExec(new Runnable() {

            @Override
            public void run() {
                ProjectRepositoryNode.getInstance().cleanup();
                // ProjectManager.getInstance().cleanupViewProjects();

                IRepositoryView repositoryView = RepositoryManagerHelper.findRepositoryView();
                if (repositoryView instanceof CommonNavigator) {
                    CommonViewer commonViewer = ((CommonNavigator) repositoryView).getCommonViewer();
                    Object input = commonViewer.getInput();
                    // make sure to init the repository view rightly.
                    commonViewer.setInput(input);
                }
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
     * org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        // nothing to do when selection changes.
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.IViewActionDelegate#init(org.eclipse.ui.IViewPart)
     */
    @Override
    public void init(IViewPart theView) {
        view = (RepoViewCommonNavigator) theView;
    }

}
