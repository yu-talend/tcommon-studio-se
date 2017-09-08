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

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.ui.runtime.expressionbuilder.ICellEditorDialog;
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
public class InstallModuleDialog extends Dialog implements ICellEditorDialog {

    private Text jarPathTxt;

    private Text originalUriTxt;

    private Text customUriText;

    private Button useCustomBtn;

    private Button detectButton;

    private Label errorLabel;

    private ModuleNeeded module;

    private CustomURITextCellEditor cellEditor;

    // match mvn:group-id/artifact-id/version/type/classifier
    public static final String expression1 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}/)(\\w+/)(\\w+))";//$NON-NLS-1$

    // match mvn:group-id/artifact-id/version/type
    public static final String expression2 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}/)\\w+)";//$NON-NLS-1$

    // match mvn:group-id/artifact-id/version
    public static final String expression3 = "(mvn:(\\w+.*/)(\\w+.*/)([0-9]+(\\.[0-9])+(-SNAPSHOT){0,1}))";//$NON-NLS-1$

    private PatternMatcherInput patternMatcherInput;

    private Perl5Matcher matcher = new Perl5Matcher();

    private Perl5Compiler compiler = new Perl5Compiler();

    private Pattern pattern;

    /**
     * DOC wchen InstallModuleDialog constructor comment.
     * 
     * @param parentShell
     */
    public InstallModuleDialog(Shell parentShell, ModuleNeeded module) {
        super(parentShell);
        setShellStyle(SWT.CLOSE | SWT.MAX | SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL | SWT.RESIZE | getDefaultOrientation());
        this.module = module;
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
        if (cellEditor != null) {
            this.module = cellEditor.getModule();
        }
        ((GridData) parent.getLayoutData()).minimumWidth = 600;
        ((GridData) parent.getLayoutData()).heightHint = 300;
        GridData data = new GridData(GridData.FILL_BOTH);
        Composite container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginTop = 10;
        layout.marginLeft = 20;
        layout.marginRight = 20;
        layout.numColumns = 3;
        container.setLayout(layout);
        container.setLayoutData(data);

        Label label1 = new Label(container, SWT.NONE);
        label1.setText(Messages.getString("InstallModuleDialog.newJar"));
        jarPathTxt = new Text(container, SWT.BORDER);
        jarPathTxt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL));

        Button browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("...");//$NON-NLS-1$
        browseButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                handleButtonPressed();
            }
        });

        Label label2 = new Label(container, SWT.NONE);
        label2.setText(Messages.getString("InstallModuleDialog.originalUri"));
        originalUriTxt = new Text(container, SWT.BORDER);
        GridData gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 2;
        originalUriTxt.setLayoutData(gdData);
        originalUriTxt.setEnabled(false);
        originalUriTxt.setBackground(container.getBackground());
        originalUriTxt.setText(module.getMavenUri());

        Composite customContainter = new Composite(container, SWT.NONE);
        customContainter.setLayoutData(new GridData());
        layout = new GridLayout();
        layout.marginTop = 0;
        layout.marginLeft = 0;
        layout.marginRight = 0;
        layout.numColumns = 2;
        customContainter.setLayout(layout);

        useCustomBtn = new Button(customContainter, SWT.CHECK);
        gdData = new GridData();
        useCustomBtn.setLayoutData(gdData);

        Label label3 = new Label(customContainter, SWT.NONE);
        label3.setText(Messages.getString("InstallModuleDialog.customUri"));
        customUriText = new Text(container, SWT.BORDER);
        gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 2;
        customUriText.setLayoutData(gdData);
        customUriText.setEnabled(useCustomBtn.getSelection());
        final String customMavenUri = module.getCustomMavenUri();
        if (cellEditor != null) {
            useCustomBtn.setSelection(cellEditor.getExpression().equals(customMavenUri));
            customUriText.setEnabled(useCustomBtn.getSelection());
        }
        if (customUriText.isEnabled() && customMavenUri != null) {
            customUriText.setText(customMavenUri);
        }
        detectButton = new Button(container, SWT.NONE);
        detectButton.setText("Dectect Maven URI");
        detectButton.setEnabled(false);
        gdData = new GridData(GridData.FILL_HORIZONTAL | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 3;
        detectButton.setLayoutData(gdData);

        errorLabel = new Label(container, SWT.WRAP);
        gdData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 3;
        errorLabel.setLayoutData(gdData);
        errorLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));

        jarPathTxt.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                checkError();
            }
        });

        useCustomBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (useCustomBtn.getSelection()) {
                    customUriText.setEnabled(true);
                    if (module.getCustomMavenUri() == null) {
                        customUriText.setText("mvn:org.talend.libraries/");//$NON-NLS-1$
                    } else {
                        customUriText.setText(module.getCustomMavenUri());
                    }
                } else {
                    customUriText.setEnabled(false);
                    customUriText.setText("");
                }
                checkError();
            }
        });

        customUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                checkError();
            }
        });
        detectButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                checkMavenRepository();
            }
        });

        return parent;
    }

    private void checkError() {
        String errorMessage = null;
        boolean needDetect = false;
        if (useCustomBtn.getSelection()) {
            // validate custom uri + check status
            String customText = customUriText.getText().trim();
            String originalText = originalUriTxt.getText().trim();
            if (customText.equals(originalText)
                    || MavenUrlHelper.addTypeForMavenUri(customText, module.getModuleName()).equals(originalText)) {
                errorMessage = Messages.getString("InstallModuleDialog.error.sameCustomURI");
            } else {
                patternMatcherInput = new PatternMatcherInput(customText);
                matcher.setMultiline(false);
                boolean isMatch = matcher.matches(patternMatcherInput, pattern);
                if (!isMatch) {
                    errorMessage = Messages.getString("InstallModuleDialog.error.customURI");
                } else {
                    String customURIWithType = MavenUrlHelper.addTypeForMavenUri(customText, module.getModuleName());
                    ELibraryInstallStatus status = ModuleStatusProvider.getDeployStatus(customURIWithType);
                    if (status == ELibraryInstallStatus.DEPLOYED) {
                        errorMessage = Messages.getString("InstallModuleDialog.error.jarexsit");
                    }
                    if (status == null) {
                        needDetect = true;
                    }
                }
            }
        } else {
            ELibraryInstallStatus status = module.getStatus();
            if (status == ELibraryInstallStatus.DEPLOYED) {
                errorMessage = Messages.getString("InstallModuleDialog.error.jarexsit");
            } else {
                if (module.getCustomMavenUri() == null) {
                    if (!new File(jarPathTxt.getText()).exists()) {
                        errorMessage = Messages.getString("InstallModuleDialog.error.jarPath");
                    }
                }
            }
        }
        if (errorMessage != null) {
            getButton(IDialogConstants.OK_ID).setEnabled(false);
            errorLabel.setText(errorMessage);
        } else {
            NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
            if (customNexusServer == null && !needDetect) {
                getButton(IDialogConstants.OK_ID).setEnabled(true);
                errorLabel.setText("");
            } else {
                errorLabel.setText(Messages.getString("InstallModuleDialog.error.detectMvnURI"));
                getButton(IDialogConstants.OK_ID).setEnabled(false);
                detectButton.setEnabled(true);
            }
        }
    }

    private void checkMavenRepository() {
        String mvnURI = null;
        if (useCustomBtn.getSelection()) {
            mvnURI = customUriText.getText().trim();
        } else {
            mvnURI = originalUriTxt.getText().trim();
        }
        ILibraryManagerService libManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                ILibraryManagerService.class);
        boolean exsit = false;
        String jarPathFromMaven = libManagerService.getJarPathFromMaven(mvnURI);
        if (jarPathFromMaven != null) {
            exsit = true;
        } else {
            NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
            if (customNexusServer != null) {
                try {
                    File resolveJar = libManagerService.resolveJar(customNexusServer, mvnURI);
                    if (resolveJar != null) {
                        exsit = true;
                    }
                } catch (Exception e) {
                    exsit = false;
                }
            }
        }
        if (exsit) {
            errorLabel.setText(Messages.getString("InstallModuleDialog.error.jarexsit"));
            getButton(IDialogConstants.OK_ID).setEnabled(false);
        } else {
            errorLabel.setText("");
            getButton(IDialogConstants.OK_ID).setEnabled(true);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Control control = super.createContents(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
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
        open();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    @Override
    protected void okPressed() {
        if (useCustomBtn.getSelection()) {
            if (!useCustomBtn.getText().equals(module.getCustomMavenUri())) {
                if (cellEditor != null) {
                    cellEditor.setConsumerExpression(customUriText.getText().trim());
                    cellEditor.fireApplyEditorValue();
                } else {
                    module.setCustomMavenUri(customUriText.getText().trim());
                }
            }
        } else {
            if (cellEditor != null) {
                cellEditor.setConsumerExpression(originalUriTxt.getText());
                cellEditor.fireApplyEditorValue();
            }
        }
        String mvnUri = module.getCustomMavenUri();
        if (mvnUri == null) {
            mvnUri = module.getMavenUri();
        }
        if (jarPathTxt.getText() != null) {
            File file = new File(jarPathTxt.getText().trim());
            if (file.exists()) {
                try {
                    LibManagerUiPlugin.getDefault().getLibrariesService().deployLibrary(file.toURL(), mvnUri);
                } catch (Exception e) {
                    ExceptionHandler.process(e);
                }
            }
        }
        super.okPressed();
        LibManagerUiPlugin.getDefault().getLibrariesService().checkLibraries();
    }
}
