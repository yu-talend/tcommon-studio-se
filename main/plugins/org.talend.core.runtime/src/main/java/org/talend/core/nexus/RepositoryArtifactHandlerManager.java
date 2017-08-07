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
package org.talend.core.nexus;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.talend.commons.exception.ExceptionHandler;

/**
 * created by wchen on Aug 3, 2017 Detailled comment
 *
 */
public class RepositoryArtifactHandlerManager {

    private static Map<String, IRepositoryArtifactHandler> handlers = null;

    private static IRepositoryArtifactHandler officialHandler = null;

    private synchronized static void initHandlers() {
        if (handlers == null) {
            handlers = new HashMap<String, IRepositoryArtifactHandler>();
            IExtensionRegistry extensionRegistry = Platform.getExtensionRegistry();
            IExtensionPoint extensionPoint = extensionRegistry.getExtensionPoint("org.talend.core.runtime.artifact_handler"); //$NON-NLS-1$
            if (extensionPoint != null) {
                IExtension[] extensions = extensionPoint.getExtensions();
                for (IExtension extension : extensions) {
                    IConfigurationElement[] configurationElements = extension.getConfigurationElements();
                    for (IConfigurationElement configurationElement : configurationElements) {
                        try {
                            String type = configurationElement.getAttribute("type");
                            Object object = configurationElement.createExecutableExtension("class"); //$NON-NLS-1$
                            if (object instanceof IRepositoryArtifactHandler) {
                                IRepositoryArtifactHandler handler = (IRepositoryArtifactHandler) object;
                                handlers.put(type, handler);
                            }
                        } catch (CoreException e) {
                            ExceptionHandler.process(e);
                        }
                    }
                }
            }
        }
    }

    public static IRepositoryArtifactHandler getCustomerRepositoryHander() {
        initHandlers();
        NexusServerBean customNexusServer = TalendLibsServerManager.getInstance().getCustomNexusServer();
        if (customNexusServer != null) {
            IRepositoryArtifactHandler repHandler = handlers.get(customNexusServer.getType());
            if (repHandler != null) {
                repHandler.setArtifactServerBean(customNexusServer);
            }
            if (repHandler.checkConnection()) {
                return repHandler;
            }
        }
        return null;
    }

    public static IRepositoryArtifactHandler getTalendRepositoryHander() {
        initHandlers();
        NexusServerBean talendServer = TalendLibsServerManager.getInstance().getTalentArtifactServer();
        if (talendServer != null) {
            IRepositoryArtifactHandler repHandler = handlers.get(talendServer.getType());
            if (repHandler != null) {
                repHandler = repHandler.clone();
                repHandler.setArtifactServerBean(talendServer);
            }
            return repHandler;
        }
        return null;
    }

    public static IRepositoryArtifactHandler getSoftwareUpdateRepositoryHandler(String adminUrl, String userName, String password) {
        initHandlers();
        NexusServerBean updateServer = TalendLibsServerManager.getInstance().getSoftwareUpdateNexusServer(adminUrl, userName,
                password);
        if (updateServer != null) {
            IRepositoryArtifactHandler repHandler = handlers.get(updateServer.getType());
            if (repHandler != null) {
                repHandler = repHandler.clone();
                repHandler.setArtifactServerBean(updateServer);
            }
            if (repHandler.checkConnection()) {
                return repHandler;
            }
        }
        return null;

    }

}
