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
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.swt.dialogs.IConfigModuleDialog;
import org.talend.core.GlobalServiceRegister;
import org.talend.core.ILibraryManagerService;
import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.librariesmanager.model.ModulesNeededProvider;
import org.talend.librariesmanager.ui.LibManagerUiPlugin;
import org.talend.librariesmanager.ui.i18n.Messages;

/**
 * 
 * created by wchen on Sep 18, 2017 Detailled comment
 *
 */
public class ConfigModuleDialog extends InstallModuleDialog implements IConfigModuleDialog {

    private Text nameTxt;

    private Button platfromRadioBtn;

    private Combo platformCombo;

    private Button repositoryRadioBtn;

    private Button installRadioBtn;

    private Button findRadioBtn;

    private ConfigModuleURIComposite findExsitURIComposite;

    private String initValue;

    private GridData installNewLayoutData;

    private GridData findExistLayoutdata;

    private Composite repGroupSubComp;

    private Composite installNewContainer;

    private Composite findExistContainer;

    private String urlToUse;

    /**
     * DOC wchen InstallModuleDialog constructor comment.
     * 
     * @param parentShell
     */
    public ConfigModuleDialog(Shell parentShell, String initValue) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE | getDefaultOrientation());
        this.initValue = initValue;
        if (initValue != null && !"".equals(initValue)) {
            moduleName = initValue;
            ModuleNeeded testModuel = new ModuleNeeded("", initValue, "", true);
            defaultURIValue = testModuel.getDefaultMavenURI();
            String customMavenUri = testModuel.getCustomMavenUri();
            if (customMavenUri != null) {
                cusormURIValue = customMavenUri;
            }
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.getString("ConfigModuleDialog.text"));//$NON-NLS-1$
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        GridData data = new GridData(GridData.FILL_BOTH);
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginTop = 10;
        layout.marginLeft = 20;
        layout.marginRight = 20;
        layout.marginBottom = 100;
        container.setLayout(layout);
        container.setLayoutData(data);
        createWarningLabel(container);
        createPlatformGroup(container);
        createRepositoryGroup(container);
        return parent;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        setPlatformGroupEnabled(true);
        setRepositoryGroupEnabled(false);
        return control;
    }

    private void createPlatformGroup(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);

        platfromRadioBtn = new Button(composite, SWT.RADIO);
        platfromRadioBtn.setText(Messages.getString("ConfigModuleDialog.platfromBtn"));
        platfromRadioBtn.setSelection(true);

        platformCombo = new Combo(composite, SWT.READ_ONLY);
        platformCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        platfromRadioBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setPlatformGroupEnabled(true);
                setRepositoryGroupEnabled(false);
            }
        });

        Set<String> jarsAvailable = new HashSet<String>();
        Set<ModuleNeeded> unUsedModules = ModulesNeededProvider.getAllManagedModules();
        for (ModuleNeeded module : unUsedModules) {
            if (module.getStatus() == ELibraryInstallStatus.INSTALLED) {
                jarsAvailable.add(module.getModuleName());
            }
        }
        String[] moduleValueArray = jarsAvailable.toArray(new String[jarsAvailable.size()]);
        Comparator<String> comprarator = new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        };
        Arrays.sort(moduleValueArray, comprarator);
        platformCombo.setItems(moduleValueArray);
        if (jarsAvailable.contains(initValue)) {
            platformCombo.setText(initValue);
        } else {
            platformCombo.setText(moduleValueArray[0]);
        }
    }

    private void setPlatformGroupEnabled(boolean enable) {
        platfromRadioBtn.setSelection(enable);
        platformCombo.setEnabled(enable);
        if (enable) {
            setMessage(Messages.getString("ConfigModuleDialog.message"), IMessageProvider.INFORMATION);
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
    }

    private void createRepositoryGroup(Composite container) {
        Composite composite = new Composite(container, SWT.NONE);
        GridLayout layout = new GridLayout();
        composite.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        composite.setLayoutData(data);

        repositoryRadioBtn = new Button(composite, SWT.RADIO);
        repositoryRadioBtn.setText(Messages.getString("ConfigModuleDialog.repositoryBtn"));
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        repositoryRadioBtn.setLayoutData(data);

        repGroupSubComp = new Composite(container, SWT.NONE);
        layout = new GridLayout();
        layout.marginLeft = 25;
        repGroupSubComp.setLayout(layout);
        data = new GridData(GridData.FILL_BOTH);
        repGroupSubComp.setLayoutData(data);

        installRadioBtn = new Button(repGroupSubComp, SWT.RADIO);
        installRadioBtn.setText(Messages.getString("ConfigModuleDialog.installNewBtn"));
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        installRadioBtn.setLayoutData(data);
        installRadioBtn.setSelection(true);

        findRadioBtn = new Button(repGroupSubComp, SWT.RADIO);
        findRadioBtn.setText(Messages.getString("ConfigModuleDialog.findExistBtn"));
        data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        findRadioBtn.setLayoutData(data);

        installNewContainer = new Composite(repGroupSubComp, SWT.NONE);
        layout = new GridLayout();
        layout.marginLeft = 0;
        layout.numColumns = 3;
        installNewContainer.setLayout(layout);
        installNewLayoutData = new GridData(GridData.FILL_BOTH);
        installNewContainer.setLayoutData(installNewLayoutData);
        createInstallNewComposite(installNewContainer);

        findExistContainer = new Composite(repGroupSubComp, SWT.NONE);
        layout = new GridLayout();
        layout.marginLeft = 0;
        layout.numColumns = 3;
        findExistContainer.setLayout(layout);
        findExistLayoutdata = new GridData(GridData.FILL_BOTH);
        findExistContainer.setLayoutData(findExistLayoutdata);
        createFindExistingModuleComposite(findExistContainer);
        findExistLayoutdata.exclude = true;

        repositoryRadioBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setPlatformGroupEnabled(false);
                setRepositoryGroupEnabled(true);
            }
        });
        installRadioBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (installRadioBtn.getSelection()) {
                    installNewLayoutData.exclude = false;
                    installNewContainer.setVisible(true);

                    findExistLayoutdata.exclude = true;
                    findExistContainer.setVisible(false);

                    repGroupSubComp.layout();
                    checkFieldsError();
                }
            }
        });
        findRadioBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (findRadioBtn.getSelection()) {
                    installNewLayoutData.exclude = true;
                    installNewContainer.setVisible(false);

                    findExistLayoutdata.exclude = false;
                    findExistContainer.setVisible(true);

                    repGroupSubComp.layout();
                    checkFieldsError();
                }
            }

        });

    }

    private void createInstallNewComposite(Composite composite) {
        createJarPathComposite(composite);
        installNewRUIComposite = new InstallModuleURIComposite(this);
        installNewRUIComposite.setCusormURIValue(cusormURIValue);
        installNewRUIComposite.setDefaultURIValue(defaultURIValue);
        installNewRUIComposite.setModuleName(moduleName);
        installNewRUIComposite.createMavenURIComposite(composite);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.librariesmanager.ui.dialogs.InstallModuleDialog#jarPathModified()
     */
    @Override
    protected void jarPathModified() {
        File file = new File(jarPathTxt.getText());
        moduleName = file.getName();
        if (!"".equals(moduleName)) {
            installNewRUIComposite.setupMavenURIByModuleName(moduleName);
        }
        super.jarPathModified();
    }

    private void createFindExistingModuleComposite(Composite composite) {
        Label nameLabel = new Label(composite, SWT.NONE);
        nameLabel.setText(Messages.getString("ConfigModuleDialog.moduleName"));
        nameTxt = new Text(composite, SWT.BORDER);
        GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        data.horizontalSpan = 2;
        nameTxt.setLayoutData(data);
        findExsitURIComposite = new ConfigModuleURIComposite(this);
        findExsitURIComposite.setCusormURIValue(cusormURIValue);
        findExsitURIComposite.setDefaultURIValue(defaultURIValue);
        findExsitURIComposite.setModuleName(moduleName);
        findExsitURIComposite.createMavenURIComposite(composite);

        nameTxt.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                String jarName = nameTxt.getText().trim();
                if (!jarName.contains(".")) {
                    jarName = addDefaultExtension(jarName);
                    findExsitURIComposite.setupMavenURIByModuleName(jarName);
                }
                checkFieldsError();
            }
        });

    }

    private String addDefaultExtension(String jarName) {
        if (!jarName.contains(".")) {
            jarName = jarName + ".jar";
        }
        return jarName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.librariesmanager.ui.dialogs.InstallModuleDialog#checkFieldsError()
     */
    @Override
    public boolean checkFieldsError() {
        if (installRadioBtn.getSelection()) {
            return super.checkFieldsError();
        } else {
            return checkFindExsitFieldsError();
        }
    }

    protected boolean checkFindExsitFieldsError() {
        moduleName = nameTxt.getText().trim();
        if ("".equals(moduleName) || !moduleName.contains(".")) {
            setMessage(Messages.getString("ConfigModuleDialog.moduleName.error"), IMessageProvider.ERROR);
            findExsitURIComposite.detectButton.setEnabled(false);
            return false;
        }
        boolean statusOK = findExsitURIComposite.checkFieldsError();
        if (!statusOK) {
            return false;
        }

        setMessage(Messages.getString("ConfigModuleDialog.message"), IMessageProvider.INFORMATION);
        getButton(IDialogConstants.OK_ID).setEnabled(true);
        return true;
    }

    private void setRepositoryGroupEnabled(boolean enable) {
        repositoryRadioBtn.setSelection(enable);
        // install
        installRadioBtn.setEnabled(enable);
        jarPathTxt.setEnabled(enable);
        browseButton.setEnabled(enable);
        installNewRUIComposite.useCustomBtn.setEnabled(enable);
        installNewRUIComposite.customUriText.setEnabled(enable && installNewRUIComposite.useCustomBtn.getSelection());
        // find existing
        findRadioBtn.setEnabled(enable);
        nameTxt.setEnabled(enable);
        findExsitURIComposite.useCustomBtn.setEnabled(enable);
        findExsitURIComposite.customUriText.setEnabled(enable && findExsitURIComposite.useCustomBtn.getSelection());
        if (enable) {
            checkFieldsError();

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        if (platfromRadioBtn.getSelection()) {
            moduleName = platformCombo.getText();
        } else if (repositoryRadioBtn.getSelection()) {
            String originalURI = null;
            String customURI = null;
            if (installRadioBtn.getSelection()) {
                final File file = new File(jarPathTxt.getText().trim());
                moduleName = file.getName();
                originalURI = installNewRUIComposite.defaultUriTxt.getText().trim();
                if (installNewRUIComposite.useCustomBtn.getSelection()) {
                    customURI = MavenUrlHelper.addTypeForMavenUri(installNewRUIComposite.customUriText.getText().trim(),
                            moduleName);
                }
                urlToUse = !"".equals(customURI) ? customURI : originalURI;
                final IRunnableWithProgress acceptOursProgress = new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            monitor.beginTask("Install module " + file.getName(), 100);
                            monitor.worked(10);
                            LibManagerUiPlugin.getDefault().getLibrariesService().deployLibrary(file.toURL(), urlToUse);
                            monitor.done();
                        } catch (IOException e) {
                            ExceptionHandler.process(e);
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
            } else if (findRadioBtn.getSelection()) {
                originalURI = findExsitURIComposite.defaultUriTxt.getText().trim();
                moduleName = nameTxt.getText().trim();
                if (findExsitURIComposite.useCustomBtn.getSelection()) {
                    customURI = MavenUrlHelper.addTypeForMavenUri(findExsitURIComposite.customUriText.getText().trim(),
                            moduleName);
                }
                urlToUse = !"".equals(customURI) ? customURI : originalURI;
            }
            Set<String> modulesNeededNames = ModulesNeededProvider.getModulesNeededNames();
            if (!modulesNeededNames.contains(moduleName)) {
                ModulesNeededProvider.addUnknownModules(moduleName, urlToUse, false);
            } else {
                // change the custom uri
                ModuleNeeded testModule = new ModuleNeeded("", "", true, originalURI);
                testModule.setCustomMavenUri(customURI);
                ILibraryManagerService libManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault()
                        .getService(ILibraryManagerService.class);
                libManagerService.saveCustomMavenURIMap();
            }
            LibManagerUiPlugin.getDefault().getLibrariesService().checkLibraries();
        }
        setReturnCode(OK);
        close();
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.commons.ui.swt.dialogs.IConfigModuleDialog#getMavenURI()
     */
    @Override
    public String getMavenURI() {
        return urlToUse;
    }
}
