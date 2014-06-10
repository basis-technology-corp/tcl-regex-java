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
import org.junit.Ignore;
import org.junit.Test;

/**
 * some 'pons asinorum' tests to see if the runtime works at all.
 */
public class SimpleRuntimeTest extends Assert {

    RegExp compile(String pattern, EnumSet<PatternFlags> flags) throws RegexException {
        return Compiler.compile(pattern, flags);
    }

    boolean doMatch(RegExp exp, String input) throws RegexException {
        Runtime runtime = new Runtime();
        return runtime.exec(exp, input.toCharArray(), 0, input.length(), EnumSet.noneOf(ExecFlags.class));
    }

    boolean doMatch(RegExp exp, String input, EnumSet<ExecFlags> flags) throws RegexException {
        Runtime runtime = new Runtime();
        return runtime.exec(exp, input.toCharArray(), 0, input.length(), flags);
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

    @Test
    public void testSingleDotMatch() throws Exception {
        RegExp exp = compile(".", EnumSet.of(PatternFlags.BASIC));
        assertTrue(doMatch(exp, "a"));
    }

    @Test
    public void testAlternation() throws Exception {
        RegExp exp = compile("a|b", EnumSet.of(PatternFlags.ADVANCED));
        assertTrue(doMatch(exp, "a"));
        assertTrue(doMatch(exp, "b"));
        assertFalse(doMatch(exp, "c"));
    }

    @Test
    public void testMoreDots() throws Exception {
        RegExp exp = compile("a.b..c", EnumSet.of(PatternFlags.BASIC));
        assertTrue(doMatch(exp, "aXbYYc"));
        assertFalse(doMatch(exp, "abYYc"));
    }

    @Test
    public void testQuest() throws Exception {
        // ? is advanced?
        RegExp exp = compile("ab?c", EnumSet.of(PatternFlags.ADVANCED));
        assertTrue(doMatch(exp, "abc"));
        assertTrue(doMatch(exp, "ac"));
        assertFalse(doMatch(exp, "abbc"));
    }

    @Test
    public void testQuant() throws Exception {
        // ? is advanced?
        RegExp exp = compile("ab{1,2}c", EnumSet.of(PatternFlags.ADVANCED));
        assertTrue(doMatch(exp, "abc"));
        assertTrue(doMatch(exp, "XabcY"));
        assertTrue(doMatch(exp, "abbc"));
        assertFalse(doMatch(exp, "ac"));
        assertFalse(doMatch(exp, "abbbc"));
    }

    @Test
    public void testAnchors() throws Exception {
        RegExp exp = compile("^abc$", EnumSet.of(PatternFlags.ADVANCED));
        assertTrue(doMatch(exp, "abc"));
        assertFalse(doMatch(exp, "XabcY"));

        assertFalse(doMatch(exp, "abc", EnumSet.of(ExecFlags.NOTBOL)));
        assertFalse(doMatch(exp, "abc", EnumSet.of(ExecFlags.NOTEOL)));
    }

    @Test
    public void testCapture() throws Exception {
        RegExp exp = compile("a(?:nonc+ap)b(ca+p)c([1-9]+)$", EnumSet.of(PatternFlags.ADVANCED));
        //..............................0000000000111111111122
        //..............................0123456789012345678901
        boolean matched = doMatch(exp, "Xanonccapbcaapc1234567");
        assertTrue(matched);
        assertEquals(3, exp.matches.size());
        assertEquals(new RegMatch(1, 22), exp.matches.get(0));
        assertEquals(new RegMatch(10, 14), exp.matches.get(1));
        assertEquals(new RegMatch(15, 22), exp.matches.get(2));
    }

    @Test
    public void testClasses() throws Exception {
        //[[:digit:]]
        RegExp exp = compile("[[:digit:]+]", EnumSet.of(PatternFlags.ADVANCED));
        assertTrue(doMatch(exp, "1234567890"));
        assertTrue(doMatch(exp, "1234567890a"));
    }

    @Ignore
    @Test
    public void testLookahead() throws Exception {
        RegExp exp = compile("^[^:]+(?=.*\\.com$)", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        boolean matched = doMatch(exp, "http://www.activestate.com");
        assertTrue(matched);
        assertEquals(2, exp.matches.size());
    }

    @Ignore
    @Test
    public void testSimpleLookahead() throws Exception {
        RegExp exp = compile("^a(?=bc$)", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        boolean matched = doMatch(exp, "abc");
        assertTrue(matched);
        assertEquals(2, exp.matches.size());
    }
}
