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
package org.talend.librariesmanager.nexus;

import org.talend.core.nexus.IRepositoryArtifactHandler;
import org.talend.core.nexus.NexusServerBean;

/**
 * created by wchen on Aug 2, 2017 Detailled comment
 *
 */
public abstract class AbstractArtifactRepositoryHandler implements IRepositoryArtifactHandler {

    protected NexusServerBean serverBean;

    /*
     * (non-Javadoc)
     * 
     * @see org.talend.core.nexus.IArtifacRepositoryHandler#setArtifactServerBean(org.talend.core.nexus.NexusServerBean)
     */
    @Override
    public void setArtifactServerBean(NexusServerBean serverBean) {
        this.serverBean = serverBean;
    }

    /**
     * Getter for serverBean.
     * 
     * @return the serverBean
     */
    @Override
    public NexusServerBean getArtifactServerBean() {
        return this.serverBean;
    }

    @Override
    public abstract IRepositoryArtifactHandler clone();
}
