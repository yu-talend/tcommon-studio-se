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
package org.talend.core.runtime.dynamic.impl;

import java.util.HashMap;
import java.util.Map;

import org.talend.core.runtime.dynamic.IDynamicExtension;
import org.talend.core.runtime.dynamic.IDynamicPlugin;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public class DynamicPlugin extends AbstractDynamicElement implements IDynamicPlugin {

    private static final String TAG_NAME = "plugin"; //$NON-NLS-1$

    private Map<String, IDynamicExtension> extensionMap;

    public DynamicPlugin() {
        extensionMap = new HashMap<>();
    }

    @Override
    public IDynamicExtension getExtension(String extensionPoint, boolean createIfNotExist) {
        IDynamicExtension extension = extensionMap.get(extensionPoint);
        if (extension == null && createIfNotExist) {
            DynamicExtension dynamicExtension = new DynamicExtension();
            dynamicExtension.setExtensionPoint(extensionPoint);
            extension = dynamicExtension;
            super.addChild(dynamicExtension);
            extensionMap.put(extensionPoint, extension);
        }
        return extension;
    }

    @Override
    public IDynamicExtension removeExtension(String extensionPoint) {
        IDynamicExtension extension = extensionMap.remove(extensionPoint);
        if (extension != null) {
            super.getChildren().remove(extension);
        }
        return extension;
    }

    public void addExtension(IDynamicExtension extension) {
        super.addChild((AbstractDynamicElement) extension);
        extensionMap.put(extension.getExtensionPoint(), extension);
    }

    @Override
    public String toXmlString() throws Exception {
        return super.toXmlJson().toString();
    }

    @Override
    protected String getTagName() {
        return TAG_NAME;
    }

    public static DynamicPlugin fromXmlJson(JSONObject json) throws Exception {

        String jsonTagName = getTagNameFrom(json);

        if (jsonTagName != null && !jsonTagName.isEmpty()) {
            if (!TAG_NAME.equals(jsonTagName)) {
                throw new Exception("Current json object is not a plugin");
            }
        }

        DynamicPlugin dynamicPlugin = new DynamicPlugin();

        dynamicPlugin.initAttributesFromXmlJson(json);

        JSONArray children = getChildrenFrom(json);
        if (children != null) {
            int length = children.length();
            for (int i = 0; i < length; ++i) {
                JSONObject jObj = children.getJSONObject(i);
                String tagName = getTagNameFrom(json);
                if (tagName == null) {
                    DynamicConfiguration config = DynamicConfiguration.fromXmlJson(jObj);
                    dynamicPlugin.addChild(config);
                } else if (DynamicExtension.TAG_NAME.equals(tagName)) {
                    DynamicExtension extension = DynamicExtension.fromXmlJson(json);
                    dynamicPlugin.addExtension(extension);
                } else {
                    DynamicConfiguration config = DynamicConfiguration.fromXmlJson(jObj);
                    dynamicPlugin.addChild(config);
                }
            }
        }

        return dynamicPlugin;
    }

}
