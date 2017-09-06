package org.talend.repository;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

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
public class ReferenceProjectProblemManagerTest {

    @Test
    public void testCheckCycleReference() {
        Map<String, List<String>> referenceMap = new HashMap<String, List<String>>();

        List<String> rList = new ArrayList<String>();
        rList.add("R1");
        rList.add("R2");
        rList.add("R0");
        referenceMap.put("R", rList);

        List<String> r1List = new ArrayList<String>();
        rList.add("R2");
        referenceMap.put("R1", r1List);

        assertTrue(ReferenceProjectProblemManager.checkCycleReference(referenceMap));

        List<String> r0List = new ArrayList<String>();
        r0List.add("R1");
        referenceMap.put("R0", r0List);
        assertTrue(ReferenceProjectProblemManager.checkCycleReference(referenceMap));

        r0List.add("R");
        assertTrue(!ReferenceProjectProblemManager.checkCycleReference(referenceMap));
    }

}
