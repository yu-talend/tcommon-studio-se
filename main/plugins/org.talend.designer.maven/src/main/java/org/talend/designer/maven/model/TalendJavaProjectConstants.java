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
package org.talend.designer.maven.model;

import org.talend.core.model.repository.ERepositoryObjectType;

/**
 * DOC zwxue class global comment. Detailled comment
 */
public class TalendJavaProjectConstants {
    
    public static final String DIR_POMS = "poms"; //$NON-NLS-1$

    public static final String DIR_CODES = "code"; //$NON-NLS-1$

    public static final String DIR_ROUTINES = "routines"; //$NON-NLS-1$

    public static final String DIR_PIGUDFS = "pigudf"; //$NON-NLS-1$

    public static final String DIR_BEANS = "beans"; //$NON-NLS-1$
    
    public static final String DIR_JOBS = "jobs"; //$NON-NLS-1$

    public static final String DIR_PROCESS = ERepositoryObjectType.PROCESS.getFolder();

    public static final String DIR_PROCESS_MR = ERepositoryObjectType.PROCESS_MR.getFolder();

    public static final String DIR_PROCESS_STORM = ERepositoryObjectType.PROCESS_STORM.getFolder();
    
    public static final String CLASSPATH_FILE_NAME = ".classpath"; //$NON-NLS-1$

    public static final String PROJECT_FILE_NAME = ".project"; //$NON-NLS-1$

}
