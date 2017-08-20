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
package org.talend.librariesmanager.model.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLParserPoolImpl;
import org.talend.commons.exception.CommonExceptionHandler;
import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.LoginException;
import org.talend.commons.exception.PersistenceException;
import org.talend.commons.runtime.model.emf.EmfHelper;
import org.talend.commons.utils.workbench.resources.ResourceUtils;
import org.talend.core.model.general.Project;
import org.talend.core.runtime.CoreRuntimePlugin;
import org.talend.librariesmanager.emf.librariesindex.CustomURIMap;
import org.talend.librariesmanager.emf.librariesindex.LibrariesindexFactory;
import org.talend.librariesmanager.emf.librariesindex.LibrariesindexPackage;
import org.talend.librariesmanager.emf.librariesindex.util.LibrariesindexResourceFactoryImpl;
import org.talend.repository.ProjectManager;
import org.talend.repository.RepositoryWorkUnit;
import org.talend.repository.model.IProxyRepositoryFactory;

/**
 * created by wchen on Aug 18, 2017 Detailled comment
 *
 */
public class CustomUriManager {

    private CustomURIMap customURIMap;

    private static CustomUriManager manager = new CustomUriManager();;

    private static final String CUSTOM_URI_MAP = "CustomURIMap.xml";

    private CustomUriManager() {
        customURIMap = loadResources(getResourcePath(), CUSTOM_URI_MAP, true);
    }

    public static CustomUriManager getInstance() {
        return manager;
    }

    private synchronized CustomURIMap loadResources(String path, String fileName, boolean create) {
        CustomURIMap customURIMap = null;
        if (create && !new File(path + "/" + fileName).exists()) {
            customURIMap = LibrariesindexFactory.eINSTANCE.createCustomURIMap();
        } else {
            try {
                Resource resource = createLibrariesIndexResource(path, fileName);
                Map optionMap = new HashMap();
                optionMap.put(XMLResource.OPTION_DEFER_ATTACHMENT, Boolean.TRUE);
                optionMap.put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
                optionMap.put(XMLResource.OPTION_USE_PARSER_POOL, new XMLParserPoolImpl());
                optionMap.put(XMLResource.OPTION_USE_XML_NAME_TO_FEATURE_MAP, new HashMap());
                optionMap.put(XMLResource.OPTION_USE_DEPRECATED_METHODS, Boolean.FALSE);
                resource.load(optionMap);
                customURIMap = (CustomURIMap) EcoreUtil.getObjectByType(resource.getContents(),
                        LibrariesindexPackage.eINSTANCE.getCustomURIMap());
            } catch (IOException e) {
                CommonExceptionHandler.process(e);
            }
        }
        return customURIMap;

    }

    private void saveResource(CustomURIMap customMap, String filePath, String fileName) {
        try {
            Resource resource = createLibrariesIndexResource(filePath, fileName);
            resource.getContents().add(customMap);
            EmfHelper.saveResource(customMap.eResource());
        } catch (PersistenceException e1) {
            CommonExceptionHandler.process(e1);
        }
    }

    public void saveCustomURIMap() {
        final RepositoryWorkUnit repositoryWorkUnit = new RepositoryWorkUnit(ProjectManager.getInstance().getCurrentProject(),
                "Save custom maven uri map") {

            @Override
            public void run() throws PersistenceException, LoginException {
                saveResource(customURIMap, getResourcePath(), CUSTOM_URI_MAP);
            }
        };
        IProxyRepositoryFactory factory = CoreRuntimePlugin.getInstance().getProxyRepositoryFactory();
        factory.executeRepositoryWorkUnit(repositoryWorkUnit);

    }

    private Resource createLibrariesIndexResource(String installLocation, String fileName) {
        URI uri = URI.createFileURI(installLocation).appendSegment(fileName);
        LibrariesindexResourceFactoryImpl indexFact = new LibrariesindexResourceFactoryImpl();
        return indexFact.createResource(uri);
    }

    private String getResourcePath() {
        try {
            Project currentProject = ProjectManager.getInstance().getCurrentProject();
            IProject project = ResourceUtils.getProject(currentProject);
            IFolder settingsFolder = project.getFolder(".settings");
            return settingsFolder.getLocation().toPortableString();
        } catch (PersistenceException e) {
            ExceptionHandler.process(e);
        }
        return null;
    }

    public void put(String key, String value) {
        customURIMap.getUriMap().put(key, value);
    }

    public String get(String key) {
        return customURIMap.getUriMap().get(key);
    }

    public void importSettings(String filePath, String fileName) throws Exception {
        CustomURIMap loadResources = loadResources(filePath, fileName, false);
        if (loadResources != null) {
            customURIMap.getUriMap().putAll(loadResources.getUriMap());
        } else {
            throw new Exception("Can't load the settings file :" + fileName);
        }
        saveCustomURIMap();
    }

    public void exportSettings(String filePath, String fileName) {
        saveResource(customURIMap, filePath, fileName);
    }

}
