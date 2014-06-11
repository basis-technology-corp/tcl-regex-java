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
 * More examples of ranges to exercise the color map
 * TODO: figure out what case causes okcolors to promote (and thus exercise the 'free' code).
 */
public class RangeTest extends Utils {

    @Test
    public void testKitchenSink() throws Exception {
        RegExp exp = compile("[^a][\u4e00-\uf000][[:upper:]][^\ufeff][\u4e00-\u4e10]b.c.d", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "Q\u4e01A$\u4e09bGcHd"));
    }

    @Test
    public void testNegativeRange() throws Exception {
        RegExp exp = compile("[^a]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "Q"));
        assertFalse(doMatch(exp, "a"));
    }

    @Test
    public void testUnicodeRange() throws Exception {
        RegExp exp = compile("[\u4e00-\uf000]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "\u4e01"));
    }

    @Test
    public void testNotBom() throws Exception {
        RegExp exp = compile("[^\ufeff]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "$"));
    }

    @Test
    public void testUpper() throws Exception {
        RegExp exp = compile("[[:upper:]]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "A"));
    }
}
