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
import org.talend.core.nexus.TalendLibsServerManager;
import org.talend.core.runtime.maven.MavenArtifact;
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

    private Label errorLabel;

    private ModuleNeeded module;

    private CustomURITextCellEditor cellEditor;

    // match mvn:group-id/artifact-id/version/type/classifier
    private String expression1 = "(mvn:(\\w+.*/)(\\w+.*/)(([0-9]+\\.)+[0-9](-SNAPSHOT){0,1}/)(\\w+/)(\\w+))";

    // match mvn:group-id/artifact-id/version/type
    private String expression2 = "(mvn:(\\w+.*/)(\\w+.*/)(([0-9]+\\.)+[0-9](-SNAPSHOT){0,1}/)\\w+)";

    // match mvn:group-id/artifact-id/version
    private String expression3 = "(mvn:(\\w+.*/)(\\w+.*/)(([0-9]+\\.)+[0-9](-SNAPSHOT){0,1}))";

    private PatternMatcherInput patternMatcherInput;

    private Perl5Matcher matcher = new Perl5Matcher();

    private Perl5Compiler compiler = new Perl5Compiler();

    private Pattern pattern;

    /**
     * DOC wchen InstallModuleDialog constructor comment.
     * 
     * @param parentShell
     */
    public InstallModuleDialog(Shell parentShell) {
        this(parentShell, null);
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

        errorLabel = new Label(container, SWT.WRAP);
        gdData = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL);
        gdData.horizontalSpan = 3;
        errorLabel.setLayoutData(gdData);
        errorLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));

        useCustomBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                customUriText.setEnabled(useCustomBtn.getSelection());
                if (customUriText.isEnabled()) {
                    if (customMavenUri != null) {
                        customUriText.setText(customMavenUri);
                    } else {
                        customUriText.setText("mvn:org.talend.libraries/");//$NON-NLS-1$
                    }
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    errorLabel.setText("");
                }
            }
        });

        customUriText.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                patternMatcherInput = new PatternMatcherInput(customUriText.getText().trim());
                matcher.setMultiline(false);
                boolean isMatch = matcher.matches(patternMatcherInput, pattern);
                if (isMatch) {
                    getButton(IDialogConstants.OK_ID).setEnabled(true);
                    errorLabel.setText("");
                } else {
                    getButton(IDialogConstants.OK_ID).setEnabled(false);
                    errorLabel.setText(Messages.getString("InstallModuleDialog.error"));
                }
            }
        });

        return parent;
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

    /**
     * Sets the module.
     * 
     * @param module the module to set
     */
    public void setModule(ModuleNeeded module) {
        this.module = module;
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
        ILibraryManagerService libManagerService = (ILibraryManagerService) GlobalServiceRegister.getDefault().getService(
                ILibraryManagerService.class);
        if (useCustomBtn.getSelection()) {
            if (customUriText.getText() != null && !useCustomBtn.getText().equals(module.getCustomMavenUri())) {
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
        String jarPathFromMaven = libManagerService.getJarPathFromMaven(mvnUri);
        if (jarPathTxt.getText() != null) {
            File file = new File(jarPathTxt.getText().trim());
            if (file.exists()) {
                try {
                    MavenArtifact parseMvnUrl = MavenUrlHelper.parseMvnUrl(mvnUri);
                    // TODO not allow to re-deploy only for remote repository now . maybe need to do the same as local
                    // latter ?
                    if (TalendLibsServerManager.getInstance().getCustomNexusServer() != null
                            && !parseMvnUrl.getVersion().endsWith(MavenUrlHelper.VERSION_SNAPSHOT)) {
                        boolean exist = false;
                        if (jarPathFromMaven == null) {
                            File resolveJar = libManagerService.resolveJar(TalendLibsServerManager.getInstance(),
                                    TalendLibsServerManager.getInstance().getCustomNexusServer(), mvnUri);
                            if (resolveJar != null) {
                                exist = true;
                            }
                        } else {
                            exist = true;
                        }
                        if (exist) {
                            getButton(IDialogConstants.OK_ID).setEnabled(false);
                            errorLabel.setText(Messages.getString("InstallModuleDialog.error.exsit"));
                            return;
                        }
                    }
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
