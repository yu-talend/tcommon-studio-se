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
package org.talend.core.runtime.dynamic;

import java.util.Dictionary;

import org.eclipse.core.runtime.RegistryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public class DynamicServiceUtil {

    public static <T> ServiceRegistration<T> registOSGiService(BundleContext context, String[] clazzes, Object service,
            Dictionary<String, T> properties) throws Exception {
        return (ServiceRegistration<T>) context.registerService(clazzes, service, properties);
    }

    public static <T> void unregistOSGiService(ServiceRegistration<T> serviceRegistration)
            throws Exception {
        serviceRegistration.unregister();
    }

    public static void addContribution(String xmlStr) {
        
        RegistryFactory.getRegistry().addContribution(null, null, false, null, null, null);
    }
}
