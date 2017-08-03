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
package org.talend.commons.exception;

/**
 * Defines system exception - try to login invalid project
 * 
 * $Id$
 * 
 */
public class InvalidProjectException extends SystemException {

    private static final long serialVersionUID = -3332713414083531212L;

    public InvalidProjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidProjectException(Throwable cause) {
        super(cause);
    }

    public InvalidProjectException(String message) {
        super(message);
    }
}
