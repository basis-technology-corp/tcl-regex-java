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

import org.junit.Test;

/**
 * patterns compiled with case-insensitivity.
 * TODO: tricky cases like the Turkish I.
 */
public class SingleCaseTests extends Utils {
    @Test
    public void testAlternation() throws Exception {
        RegExp exp = compile("a|b", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.ICASE));
        assertTrue(doMatch(exp, "a"));
        assertTrue(doMatch(exp, "A"));
        assertTrue(doMatch(exp, "b"));
        assertTrue(doMatch(exp, "B"));
        assertFalse(doMatch(exp, "c"));
        assertFalse(doMatch(exp, "C"));
    }

    @Test
    public void testRange() throws Exception {
        RegExp exp = compile("[a-z]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.ICASE));
        assertTrue(doMatch(exp, "a"));
        assertTrue(doMatch(exp, "A"));
        assertTrue(doMatch(exp, "q"));
        assertTrue(doMatch(exp, "Q"));
        assertFalse(doMatch(exp, "$"));
    }
}
