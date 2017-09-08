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
package org.talend.librariesmanager.ui.dialogs;

import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternMatcherInput;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;
import org.junit.Assert;
import org.junit.Test;

/**
 * created by wchen on Sep 8, 2017 Detailled comment
 *
 */
public class InstallModuleDialogExpressionTest {

    @Test
    public void testMVNExpression() throws MalformedPatternException {
        String expression = InstallModuleDialog.expression1 + "|" + InstallModuleDialog.expression2 + "|"
                + InstallModuleDialog.expression3;

        Perl5Matcher matcher = new Perl5Matcher();
        matcher.setMultiline(false);
        Perl5Compiler compiler = new Perl5Compiler();
        Pattern pattern = compiler.compile(expression);

        PatternMatcherInput patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test/6.0");
        boolean matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertTrue(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test/6.0.0/exe");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertTrue(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test/6.0.0-SNAPSHOT");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertTrue(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test/6.0.0-SNAPSHOT/jar");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertTrue(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertFalse(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertFalse(matches);

        patternMatcherInput = new PatternMatcherInput("mvn:org.talend.libraries/test/6");
        matches = matcher.matches(patternMatcherInput, pattern);
        Assert.assertFalse(matches);

    }

}
