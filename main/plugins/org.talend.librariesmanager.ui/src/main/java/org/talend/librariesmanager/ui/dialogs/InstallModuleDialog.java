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
import java.util.concurrent.TimeoutException;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.expressionbuilder.ICellEditorDialog;
import org.talend.commons.ui.runtime.image.EImage;
import org.talend.commons.ui.runtime.image.ImageProvider;
import org.talend.commons.ui.utils.image.ColorUtils;
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

/**
 * created by wchen on Aug 16, 2017 Detailled comment
 *
 */
public class InstallModuleDialog extends TitleAreaDialog implements ICellEditorDialog {

    private Label warningLabel;

    private Composite warningComposite;

    private GridData warningLayoutData;

    protected Text jarPathTxt;

    protected Button browseButton;

    protected Text defaultUriTxt;

    protected Text customUriText;

    protected Button useCustomBtn;

    protected Button detectButton;

    private ModuleNeeded module;

    private CustomURITextCellEditor cellEditor;

    // match mvn:group-id/artifact-id/version/type/classifier
    public static final String expression1 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}/)(\\w+/)(\\w+))";//$NON-NLS-1$

    // match mvn:group-id/artifact-id/version/type
    public static final String expression2 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}/)\\w+)";//$NON-NLS-1$

    // match mvn:group-id/artifact-id/version
    public static final String expression3 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}))";//$NON-NLS-1$

    protected final String MVNURI_TEMPLET = "mvn:<groupid>/<artifactId>/<version>/<type>";

    protected String moduleName = "";

    protected String cusormURIValue = "";

    protected String defaultURIValue = "";

    protected PatternMatcherInput patternMatcherInput;

    protected Perl5Matcher matcher = new Perl5Matcher();

    protected Perl5Compiler compiler = new Perl5Compiler();

    protected Pattern pattern;

    private Color warningColor = ColorUtils.getCacheColor(new RGB(255, 175, 10));

    /**
     * DOC wchen InstallModuleDialog constructor comment.
     */
    public InstallModuleDialog(Shell parentShell) {
        super(parentShell);
        try {
            pattern = compiler.compile(expression1 + "|" + expression2 + "|" + expression3);
        } catch (MalformedPatternException e) {
            ExceptionHandler.process(e);
        }
    }

    public InstallModuleDialog(Shell parentShell, CustomURITextCellEditor cellEditor) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE | getDefaultOrientation());
        this.cellEditor = cellEditor;
        try {
            pattern = compiler.compile(expression1 + "|" + expression2 + "|" + expression3);
        } catch (MalformedPatternException e) {
            ExceptionHandler.process(e);
        }
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(Messages.getString("InstallModuleDialog.title"));//$NON-NLS-1$

    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginTop = 20;
        layout.marginLeft = 20;
        layout.marginRight = 20;
        layout.marginBottom = 100;
        layout.numColumns = 3;
        container.setLayout(layout);
        GridData data = new GridData(GridData.FILL_BOTH);
        container.setLayoutData(data);

        createWarningLabel(container);
        createJarPathComposite(container);
        createMavenURIComposite(container);
        createDetectComposite(container);

        warningLabel.setText(Messages.getString("InstallModuleDialog.warning", defaultUriTxt.getText().trim()));
        return parent;
    }

    protected void createWarningLabel(Composite container) {
        warningComposite = new Composite(container, SWT.NONE);
        warningComposite.setBackground(warningColor);
        warningLayoutData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        warningLayoutData.horizontalSpan = ((GridLayout) container.getLayout()).numColumns;
        warningComposite.setLayoutData(warningLayoutData);
        GridLayout layout = new GridLayout();
        layout.marginTop = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.numColumns = 2;
        warningComposite.setLayout(layout);
        Label imageLabel = new Label(warningComposite, SWT.NONE);
        imageLabel.setImage(ImageProvider.getImage(EImage.WARNING_ICON));
        imageLabel.setBackground(warningColor);

        warningLabel = new Label(warningComposite, SWT.WRAP);
        warningLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));
        warningLabel.setBackground(warningColor);
        warningLayoutData.exclude = true;
    }

    protected void createJarPathComposite(Composite container) {
        Label label1 = new Label(container, SWT.NONE);
        label1.setText(Messages.getString("InstallModuleDialog.newJar"));
        jarPathTxt = new Text(container, SWT.BORDER);
        jarPathTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("...");//$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleButtonPressed();
            }
        });
    }

    protected void createMavenURIComposite(Composite composite) {
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

        jarPathTxt.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                jarPathModified();
            }
        });

        useCustomBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                // show the warning if useCustomBtn select/deselect
                warningLayoutData.exclude = false;
                warningComposite.getParent().layout();
                if (useCustomBtn.getSelection()) {
                    customUriText.setEnabled(true);
                    customUriText.setText(cusormURIValue);
                } else {
                    customUriText.setEnabled(false);
                }
                checkInstallFieldsError();
            }
        });

        customUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                checkInstallFieldsError();
            }
        });
    }

    protected void jarPathModified() {
        checkInstallFieldsError();
    }

    protected void createDetectComposite(Composite composite) {
        detectButton = new Button(composite, SWT.NONE);
        detectButton.setText(Messages.getString("InstallModuleDialog.detectButton.text"));
        detectButton.setEnabled(false);
        GridData gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 3;
        detectButton.setLayoutData(gdData);
        detectButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDetectPressed();
            }
        });
    }

    protected void handleDetectPressed() {
        boolean deployed = checkInstalledStatus();
        if (deployed) {
            setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            setMessage(Messages.getString("InstallModuleDialog.message"), IMessageProvider.INFORMATION);
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }

    }

    /**
     * 
     * DOC wchen Comment method "checkInstallCompositeError".
     * 
     * @return false if has error
     */
    protected boolean checkInstallFieldsError() {
        String originalText = defaultUriTxt.getText().trim();
        String customURIWithType = MavenUrlHelper.addTypeForMavenUri(customUriText.getText(), moduleName);
        ELibraryInstallStatus status = null;
        if (useCustomBtn.getSelection()) {
            // if use custom uri:validate file path if not empty+ validate custom uri + check deploy status
            if (!"".equals(jarPathTxt.getText()) && !new File(jarPathTxt.getText()).exists()) {
                setMessage(Messages.getString("InstallModuleDialog.error.jarPath"), IMessageProvider.ERROR);
                return false;
            }

            boolean statusOK = true;
            statusOK = validateCustomMvnURI(originalText, customURIWithType);
            if (!statusOK) {
                return false;
            }

            status = getMavenURIInstallStatus(customURIWithType);
            if (status == ELibraryInstallStatus.DEPLOYED) {
                setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
                return false;
            }

        } else {
            // if use original uri: validate file path + check deploy status
            if (!new File(jarPathTxt.getText()).exists()) {
                setMessage(Messages.getString("InstallModuleDialog.error.jarPath"), IMessageProvider.ERROR);
                return false;
            }

            status = getMavenURIInstallStatus(originalText);
            if (status == ELibraryInstallStatus.DEPLOYED) {
                setMessage(Messages.getString("InstallModuleDialog.error.jarexsit"), IMessageProvider.ERROR);
                return false;
            }

        }

        // check deploy status from remote
        boolean statusOK = checkDetectButtonStatus();
        if (!statusOK) {
            return false;
        }

        setMessage(Messages.getString("InstallModuleDialog.message"), IMessageProvider.INFORMATION);
        return true;
    }

    protected boolean checkDetectButtonStatus() {
        NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
        if (customNexusServer == null) {
            detectButton.setEnabled(true);
            setMessage(Messages.getString("InstallModuleDialog.error.detectMvnURI"), IMessageProvider.ERROR);
            return false;
        }
        return true;
    }

    protected boolean validateCustomMvnURI(String originalText, String customText) {
        if (customText.equals(originalText)) {
            setMessage(Messages.getString("InstallModuleDialog.error.sameCustomURI"), IMessageProvider.ERROR);
            detectButton.setEnabled(false);
            return false;
        }
        patternMatcherInput = new PatternMatcherInput(customText);
        matcher.setMultiline(false);
        boolean isMatch = matcher.matches(patternMatcherInput, pattern);
        if (!isMatch) {
            setMessage(Messages.getString("InstallModuleDialog.error.customURI"), IMessageProvider.ERROR);
            detectButton.setEnabled(false);
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.TitleAreaDialog#setMessage(java.lang.String, int)
     */
    @Override
    public void setMessage(String newMessage, int newType) {
        super.setMessage(newMessage, newType);
        if (newType == IMessageProvider.ERROR) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
    }

    protected boolean checkInstalledStatus() {
        String uri = null;
        if (useCustomBtn.getSelection()) {
            uri = customUriText.getText().trim();
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        setMessage(Messages.getString("InstallModuleDialog.error.jarPath"), IMessageProvider.ERROR);
        return control;
    }

    private void handleButtonPressed() {
        FileDialog dialog = new FileDialog(getShell());
        dialog.setText(Messages.getString("InstallModuleDialog.title")); //$NON-NLS-1$

        String filePath = this.jarPathTxt.getText().trim();
        if (filePath.length() == 0) {
            dialog.setFilterPath(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
        } else {
            File file = new File(filePath);
            if (file.exists()) {
                dialog.setFilterPath(new Path(filePath).toOSString());
            }
        }

        String result = dialog.open();
        if (result != null) {
            this.jarPathTxt.setText(result);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.commons.ui.runtime.expressionbuilder.ICellEditorDialog#openDialog(java.lang.Object)
     */
    @Override
    public void openDialog(Object obj) {
        this.module = cellEditor.getModule();
        this.moduleName = module.getModuleName();
        this.defaultURIValue = module.getDefaultMavenURI();
        this.cusormURIValue = module.getCustomMavenUri();
        if (cusormURIValue == null) {
            cusormURIValue = MVNURI_TEMPLET;
        }
        open();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        String newCustomURI = null;
        if (useCustomBtn.getSelection()) {
            newCustomURI = MavenUrlHelper.addTypeForMavenUri(customUriText.getText().trim(), module.getModuleName());
            if (cellEditor != null) {
                cellEditor.setConsumerExpression(newCustomURI);
                cellEditor.fireApplyEditorValue();
            } else {
                module.setCustomMavenUri(customUriText.getText().trim());
            }
        } else {
            if (cellEditor != null) {
                cellEditor.setConsumerExpression(defaultUriTxt.getText());
                cellEditor.fireApplyEditorValue();
            }
        }

        if (!"".equals(jarPathTxt.getText().trim())) {
            String mvnUri = module.getMavenUri();
            File file = new File(jarPathTxt.getText().trim());
            if (file.exists()) {
                final IRunnableWithProgress acceptOursProgress = new IRunnableWithProgress() {

                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            monitor.beginTask("Install module " + file.getName(), 100);
                            monitor.worked(30);
                            LibManagerUiPlugin.getDefault().getLibrariesService().deployLibrary(file.toURL(), mvnUri);
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
            }
        }
        super.okPressed();
        LibManagerUiPlugin.getDefault().getLibrariesService().checkLibraries();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#close()
     */
    @Override
    public boolean close() {
        setMessage("");
        return super.close();
    }
}
