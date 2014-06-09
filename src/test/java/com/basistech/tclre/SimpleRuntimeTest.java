/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclre;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

/**
 * some 'pons asinorum' tests to see if the runtime works at all.
 */
public class SimpleRuntimeTest extends Assert {

    RegExp compile(String pattern, EnumSet<PatternFlags> flags) throws RegexException {
        Compiler compiler = new Compiler(pattern, flags);
        return compiler.compile();
    }

    boolean doMatch(RegExp exp, String input) throws RegexException {
        Runtime runtime = new Runtime();
        return runtime.exec(exp, input.toCharArray(), 0, input.length(), EnumSet.noneOf(ExecFlags.class));
    }

    @Test
    public void testDontMatch() throws Exception {
        RegExp exp = compile("a", EnumSet.of(PatternFlags.BASIC));
        assertFalse(doMatch(exp, "b"));
    }

    @Test
    public void testSingleCharMatch() throws Exception {
        RegExp exp = compile("a", EnumSet.of(PatternFlags.BASIC));
        assertTrue(doMatch(exp, "a"));
    }

}
