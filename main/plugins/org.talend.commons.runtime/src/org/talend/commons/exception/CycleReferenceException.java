package org.talend.commons.exception;
//============================================================================
//
//Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
//This source code is available under agreement available at
//%InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
//You should have received a copy of the agreement
//along with this program; if not, write to Talend SA
//9 rue Pages 92150 Suresnes, France
//
//============================================================================

public class CycleReferenceException extends BusinessException {

    private static final long serialVersionUID = 8125376947111402162L;

    public CycleReferenceException(String message) {
        super(message);
    }

}
