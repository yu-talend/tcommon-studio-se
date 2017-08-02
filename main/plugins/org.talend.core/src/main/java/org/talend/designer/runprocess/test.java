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
package org.talend.designer.runprocess;

import java.util.Set;

import org.talend.core.model.general.ModuleNeeded;
import org.talend.core.model.process.IProcess;


/**
 * created by wchen on Jul 20, 2017
 * Detailled comment
 *
 */
public class test implements IClasspathAdjuster {

    /* (non-Javadoc)
     * @see org.talend.designer.runprocess.IClasspathAdjuster#initialize()
     */
    @Override
    public void initialize() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.talend.designer.runprocess.IClasspathAdjuster#collectInfo(org.talend.core.model.process.IProcess, java.util.Set)
     */
    @Override
    public void collectInfo(IProcess process, Set<ModuleNeeded> modules) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.talend.designer.runprocess.IClasspathAdjuster#adjustClassPath(java.util.Set)
     */
    @Override
    public Set<ModuleNeeded> adjustClassPath(Set<ModuleNeeded> modulesToAjust) {
        // TODO Auto-generated method stub
        return null;
    }

}
