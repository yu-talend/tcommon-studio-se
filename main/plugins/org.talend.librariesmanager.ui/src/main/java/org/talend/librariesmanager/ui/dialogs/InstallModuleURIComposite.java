// ============================================================================
//
// Copyright (C) 2006-2014 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.librariesmanager.ui.dialogs;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.core.model.general.ModuleStatusProvider;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.TalendLibsServerManager;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.librariesmanager.ui.LibManagerUiPlugin;
import org.talend.librariesmanager.ui.i18n.Messages;
import org.talend.librariesmanager.utils.CustomMavenURIValidator;

/**
 * created by wchen on Sep 25, 2017 Detailled comment
 *
 */
public class InstallModuleURIComposite {

    Text defaultUriTxt;

    Text customUriText;

    Button useCustomBtn;

    Button detectButton;

    protected String moduleName = "";

    protected String cusormURIValue = "";

    protected String defaultURIValue = "";

    protected IInstallModuleDialog moduleDialog;

    protected final String MVNURI_TEMPLET = "mvn:<groupid>/<artifactId>/<version>/<type>";

    public InstallModuleURIComposite(IInstallModuleDialog moduleDialog) {
        this.moduleDialog = moduleDialog;

    }

    public void createMavenURIComposite(Composite composite) {
        Label label2 = new Label(composite, SWT.NONE);
        label2.setText(Messages.getString("InstallModuleDialog.originalUri"));
        defaultUriTxt = new Text(composite, SWT.BORDER);
        GridData gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 2;
        defaultUriTxt.setLayoutData(gdData);
        defaultUriTxt.setEnabled(false);
        defaultUriTxt.setBackground(composite.getBackground());
        defaultUriTxt.setText(defaultURIValue);

        Composite customContainter = new Composite(composite, SWT.NONE);
        customContainter.setLayoutData(new GridData());
        GridLayout layout = new GridLayout();
        layout.marginTop = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.numColumns = 2;
        customContainter.setLayout(layout);

        useCustomBtn = new Button(customContainter, SWT.CHECK);
        gdData = new GridData();
        useCustomBtn.setLayoutData(gdData);
        useCustomBtn.setSelection(!MVNURI_TEMPLET.equals(cusormURIValue));

        Label label3 = new Label(customContainter, SWT.NONE);
        label3.setText(Messages.getString("InstallModuleDialog.customUri"));
        customUriText = new Text(composite, SWT.BORDER);
        gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 2;
        customUriText.setLayoutData(gdData);
        customUriText.setEnabled(useCustomBtn.getSelection());
        if (customUriText.isEnabled()) {
            customUriText.setText(cusormURIValue);
        }

        detectButton = new Button(composite, SWT.NONE);
        detectButton.setText(Messages.getString("InstallModuleDialog.detectButton.text"));
        detectButton.setEnabled(false);
        gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 3;
        detectButton.setLayoutData(gdData);

        useCustomBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // show the warning if useCustomBtn select/deselect

                moduleDialog.layoutWarningComposite();
                if (useCustomBtn.getSelection()) {
                    customUriText.setEnabled(true);
                    customUriText.setText(cusormURIValue);
                } else {
                    customUriText.setEnabled(false);
                }
                moduleDialog.checkFieldsError();
            }
        });

        customUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                moduleDialog.checkFieldsError();
            }
        });

        detectButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDetectPressed();
            }
        });
    }

    protected boolean checkFieldsError() {
        String originalText = defaultUriTxt.getText().trim();
        String customURIWithType = MavenUrlHelper.addTypeForMavenUri(customUriText.getText(), moduleName);
        ELibraryInstallStatus status = null;
        if (useCustomBtn.getSelection()) {
            // if use custom uri:validate custom uri + check deploy status
            String errorMessage = CustomMavenURIValidator.validateCustomMvnURI(originalText, customURIWithType);
            if (errorMessage != null) {
                detectButton.setEnabled(false);
                moduleDialog.setMessage(errorMessage, IMessageProvider.ERROR);
                return false;
            }

            status = getMavenURIInstallStatus(customURIWithType);
            if (status == ELibraryInstallStatus.DEPLOYED) {
                moduleDialog.setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
                return false;
            }

        } else {
            status = getMavenURIInstallStatus(originalText);
            if (status == ELibraryInstallStatus.DEPLOYED) {
                moduleDialog.setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
                return false;
            }

        }

        // check deploy status from remote
        boolean statusOK = checkDetectButtonStatus();
        if (!statusOK) {
            return false;
        }

        moduleDialog.setMessage(Messages.getString("InstallModuleDialog.message"), IMessageProvider.INFORMATION);
        return true;
    }

    protected ELibraryInstallStatus getMavenURIInstallStatus(String mvnURI) {
        ELibraryInstallStatus deployStatus = ModuleStatusProvider.getDeployStatus(mvnURI);
        if (deployStatus == null) {
            ILibraryManagerService libManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                    ILibraryManagerService.class);
            libManagerService.resolveStatusLocally(mvnURI);
            deployStatus = ModuleStatusProvider.getDeployStatus(mvnURI);
        }
        return deployStatus;
    }

    protected boolean checkDetectButtonStatus() {
        NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
        if (customNexusServer == null) {
            detectButton.setEnabled(true);
            moduleDialog.setMessage(Messages.getString("InstallModuleDialog.error.detectMvnURI"), IMessageProvider.ERROR);
            return false;
        }
        return true;
    }

    protected void handleDetectPressed() {
        boolean deployed = checkInstalledStatus();
        if (deployed) {
            moduleDialog.setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
        } else {
            moduleDialog.setMessage(Messages.getString("InstallModuleDialog.message"), IMessageProvider.INFORMATION);
        }
    }

    protected boolean checkInstalledStatus() {
        String uri = null;
        if (useCustomBtn.getSelection()) {
            uri = MavenUrlHelper.addTypeForMavenUri(customUriText.getText().trim(), moduleName);
        } else {
            uri = defaultUriTxt.getText().trim();
        }
        final String mvnURI = uri;
        ILibraryManagerService libManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                ILibraryManagerService.class);
        String jarPathFromMaven = libManagerService.getJarPathFromMaven(mvnURI);
        final boolean[] deployStatus = new boolean[] { false };
        if (jarPathFromMaven != null) {
            deployStatus[0] = true;
        } else {
            final IRunnableWithProgress acceptOursProgress = new IRunnableWithProgress() {

                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
                    if (customNexusServer != null) {
                        try {

                            File resolveJar = libManagerService.resolveJar(customNexusServer, mvnURI);
                            if (resolveJar != null) {
                                deployStatus[0] = true;
                                LibManagerUiPlugin.getDefault().getLibrariesService().checkLibraries();
                            }
                        } catch (Exception e) {
                            deployStatus[0] = false;
                        }
                    }
                }
            };

            ProgressMonitorDialog dialog = new ProgressMonitorDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell());
            try {
                dialog.run(true, true, acceptOursProgress);
            } catch (Throwable e) {
                if (!(e instanceof TimeoutException)) {
                    ExceptionHandler.process(e);
                }
            }

        }

        if (useCustomBtn.getSelection() && !deployStatus[0]) {
            ModuleStatusProvider.putDeployStatus(mvnURI, ELibraryInstallStatus.NOT_DEPLOYED);
            ModuleStatusProvider.putStatus(mvnURI, ELibraryInstallStatus.NOT_INSTALLED);
        }

        return deployStatus[0];
    }

    public void setupMavenURIByModuleName(String moduleName) {
        ModuleNeeded moduel = new ModuleNeeded("", moduleName, "", true);
        defaultUriTxt.setText(moduel.getDefaultMavenURI());
        String customMavenUri = moduel.getCustomMavenUri();
        if (customMavenUri != null) {
            useCustomBtn.setSelection(true);
            customUriText.setEnabled(true);
            customUriText.setText(customMavenUri);
        }

    }

    /**
     * Sets the moduleName.
     * 
     * @param moduleName the moduleName to set
     */
    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    /**
     * Sets the cusormURIValue.
     * 
     * @param cusormURIValue the cusormURIValue to set
     */
    public void setCusormURIValue(String cusormURIValue) {
        this.cusormURIValue = cusormURIValue;
        if (cusormURIValue == null || "".equals(cusormURIValue)) {
            this.cusormURIValue = MVNURI_TEMPLET;
        }
    }

    /**
     * Sets the defaultURIValue.
     * 
     * @param defaultURIValue the defaultURIValue to set
     */
    public void setDefaultURIValue(String defaultURIValue) {
        this.defaultURIValue = defaultURIValue;
    }
}
