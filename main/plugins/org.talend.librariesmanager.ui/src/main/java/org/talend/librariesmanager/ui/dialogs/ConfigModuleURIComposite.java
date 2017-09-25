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

import org.eclipse.jface.dialogs.IMessageProvider;
import org.talend.core.model.general.ModuleNeeded.ELibraryInstallStatus;
import org.talend.core.nexus.NexusServerBean;
import org.talend.core.nexus.TalendLibsServerManager;
import org.talend.core.runtime.maven.MavenUrlHelper;
import org.talend.librariesmanager.ui.i18n.Messages;
import org.talend.librariesmanager.utils.CustomMavenURIValidator;

/**
 * created by wchen on Sep 25, 2017 Detailled comment
 *
 */
public class ConfigModuleURIComposite extends InstallModuleURIComposite {

    /**
     * DOC wchen ModuleComposite4ConfigDialog constructor comment.
     * 
     * @param moduleDialog
     * @param container
     */
    public ConfigModuleURIComposite(IInstallModuleDialog moduleDialog) {
        super(moduleDialog);
    }

    @Override
    protected void handleDetectPressed() {
        boolean deployed = checkInstalledStatus();
        if (deployed) {
            moduleDialog.setMessage(Messages.getString("ConfigModuleDialog.message"), IMessageProvider.INFORMATION);
        } else {
            moduleDialog.setMessage(Messages.getString("ConfigModuleDialog.jarNotInstalled.error"), IMessageProvider.ERROR);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.librariesmanager.ui.dialogs.ModuleComposite4InstallDialog#checkInstallFieldsError()
     */
    @Override
    protected boolean checkFieldsError() {
        String originalText = defaultUriTxt.getText().trim();
        String customURIWithType = MavenUrlHelper.addTypeForMavenUri(customUriText.getText(), moduleName);
        ELibraryInstallStatus status = null;
        if (useCustomBtn.getSelection()) {
            // if use custom uri: validate custom uri + check deploy status
            String message = CustomMavenURIValidator.validateCustomMvnURI(originalText, customURIWithType);
            if (message != null) {
                detectButton.setEnabled(false);
                moduleDialog.setMessage(message, IMessageProvider.ERROR);
                return false;
            }
            status = getMavenURIInstallStatus(customURIWithType);
        } else {
            status = getMavenURIInstallStatus(originalText);
        }

        if (status != ELibraryInstallStatus.DEPLOYED) {
            NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
            if (customNexusServer != null) {
                moduleDialog.setMessage(Messages.getString("InstallModuleDialog.error.detectMvnURI"), IMessageProvider.ERROR);
                detectButton.setEnabled(true);
                return false;
            } else {
                moduleDialog.setMessage(Messages.getString("ConfigModuleDialog.jarNotInstalled.error"), IMessageProvider.ERROR);
                return false;
            }
        }

        moduleDialog.setMessage(Messages.getString("ConfigModuleDialog.message"), IMessageProvider.INFORMATION);
        return true;
    }

}
