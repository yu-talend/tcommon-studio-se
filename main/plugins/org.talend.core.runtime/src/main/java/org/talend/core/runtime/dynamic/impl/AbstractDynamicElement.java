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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;

/**
 * DOC cmeng  class global comment. Detailled comment
 */
public abstract class AbstractDynamicElement {

    private static final String XML_TAG_NAME = "tagName"; //$NON-NLS-1$

    private static final String XML_ELEMENTS = "childNodes"; //$NON-NLS-1$

    private Map<String, Object> attributeMap;

    private List<AbstractDynamicElement> children;

    abstract protected String getTagName();

    public AbstractDynamicElement() {
        attributeMap = new HashMap<>();
        children = new ArrayList<>();
    }

    protected void setAttribute(String key, Object value) {
        attributeMap.put(key, value);
    }

    protected Object getAttribute(String key) {
        return attributeMap.get(key);
    }

    protected Map<String, Object> getAttributes() {
        return attributeMap;
    }

    protected void addChild(AbstractDynamicElement child) {
        children.add(child);
    }

    protected List<AbstractDynamicElement> getChildren() {
        return children;
    }

    protected JSONObject toXmlJson() throws Exception {
        JSONObject json = new JSONObject();

        String tagName = getTagName();
        if (tagName != null && !tagName.isEmpty()) {
            json.put(XML_TAG_NAME, tagName);
        }

        for (Map.Entry<String, Object> entry : attributeMap.entrySet()) {
            json.put(entry.getKey(), entry.getValue());
        }

        JSONArray childArray = new JSONArray();
        json.put(XML_ELEMENTS, childArray);

        for (AbstractDynamicElement child : children) {
            childArray.put(child.toXmlJson());
        }

        return json;
    }

    protected void initAttributesFromXmlJson(JSONObject json) throws Exception {
        Iterator<String> keyIter = json.keys();
        if (keyIter != null) {
            while (keyIter.hasNext()) {
                String key = keyIter.next();
                if (XML_TAG_NAME.equals(key)) {
                    continue;
                } else if (XML_ELEMENTS.equals(key)) {
                    continue;
                }
                Object value = json.opt(key);
                attributeMap.put(key, value);
            }
        }
    }

    protected static String getTagNameFrom(JSONObject xmlJson) throws Exception {
        return xmlJson.optString(XML_TAG_NAME);
    }

    protected static JSONArray getChildrenFrom(JSONObject xmlJson) throws Exception {
        return xmlJson.optJSONArray(XML_ELEMENTS);
    }
}
