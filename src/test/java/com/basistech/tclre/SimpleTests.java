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

import static com.basistech.tclre.Utils.Matches.matches;
import static com.basistech.tclre.Utils.MatcherMatches.groupIs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;

/**
 * some 'pons asinorum' tests to see if the runtime works at all.
 */
public class SimpleTests extends Utils {

    @Test
    public void testDontMatch() throws Exception {
        assertThat("b", not(matches("a", PatternFlags.BASIC)));
    }

    @Test
    public void testSingleCharMatch() throws Exception {
        assertThat("b", matches("b", PatternFlags.BASIC));
    }

    @Test
    public void testSingleDotMatch() throws Exception {
        assertThat("a", matches(".", PatternFlags.BASIC));
    }

    @Test
    public void testAlternation() throws Exception {
        RePattern exp = HsrePattern.compile("a|b", EnumSet.of(PatternFlags.ADVANCED));
        assertThat("a", matches(exp));
        assertThat("b", matches(exp));
        assertThat("c", not(matches(exp)));
    }

    @Test
    public void testMoreDots() throws Exception {
        RePattern exp = HsrePattern.compile("a.b..c", EnumSet.of(PatternFlags.BASIC));
        assertThat("aXbYYc", matches(exp));
        assertThat("abYYc", not(matches(exp)));
    }

    @Test
    public void testQuest() throws Exception {
        // ? is advanced?
        RePattern exp = HsrePattern.compile("ab?c", EnumSet.of(PatternFlags.ADVANCED));
        assertThat("abc", matches(exp));
        assertThat("ac", matches(exp));
        assertThat("abbc", not(matches(exp)));
    }

    /*
    {m}, {m,}, and {m,n}
     */

    @Test
    public void testQuant() throws Exception {
        // ? is advanced?
        RePattern exp = HsrePattern.compile("ab{1,2}cd{3,}e{2}", PatternFlags.ADVANCED);
        assertThat("abcdddee", matches(exp));
        assertThat("abcddddee", matches(exp));
        assertThat("XabcddddeeY", matches(exp));
        assertThat("abbcdddee", matches(exp));
        assertThat("acdddee", not(matches(exp)));
        assertThat("abbbcdddee", not(matches(exp)));
        assertThat("abcddee", not(matches(exp)));
        assertThat("abcddde", not(matches(exp)));

        exp = HsrePattern.compile("ab{0,1}c", PatternFlags.ADVANCED);
        assertThat("ac", matches(exp));
        assertThat("abc", matches(exp));
        assertThat("abbc", not(matches(exp)));

        exp = HsrePattern.compile("ab{0,2}c", PatternFlags.ADVANCED);
        assertThat("ac", matches(exp));
        assertThat("abc", matches(exp));
        assertThat("abbc", matches(exp));
        assertThat("abbbc", not(matches(exp)));
    }

    @Test
    public void testNullQuant() throws Exception {
        RePattern exp = HsrePattern.compile("ab{0,0}c", PatternFlags.ADVANCED);
        assertThat("ac", matches(exp));
    }

    @Test
    public void testAnchors() throws Exception {
        RePattern exp = HsrePattern.compile("^abc$", PatternFlags.ADVANCED);
        assertThat("abc", matches(exp));
        assertThat("XabcY", not(matches(exp)));

        assertThat("abc", not(matches(exp, ExecFlags.NOTBOL)));
        assertThat("abc", not(matches(exp, ExecFlags.NOTEOL)));
    }

    @Test
    public void testCapture() throws Exception {
        RePattern exp = HsrePattern.compile("a(?:nonc+ap)b(ca+p)c([1-9]+)$", PatternFlags.ADVANCED);
        String test = "Xanonccapbcaapc1234567";
        assertThat(test, matches(exp, new String[]{"caap", "1234567"}));
        ReMatcher matcher = exp.matcher(test);
        assertThat(matcher.find(), is(true));
        assertThat(matcher, groupIs(0, 1, test.length()));
    }

    @Test
    public void testClasses() throws Exception {
        //[[:digit:]]
        RePattern exp = HsrePattern.compile("[[:digit:]]+", PatternFlags.ADVANCED);
        assertThat("1234567890", matches(exp));
        assertThat("1234567890a", matches(exp));
    }

    @Test
    public void testLookahead() throws Exception {
        RePattern exp = HsrePattern.compile("^[^:]+(?=.*\\.com$)", PatternFlags.ADVANCED,
                PatternFlags.EXPANDED);
        ReMatcher matcher = exp.matcher("http://www.activestate.com");
        assertThat(matcher.find(), is(true));
        assertThat(0, equalTo(matcher.groupCount()));
        assertThat(matcher, groupIs(0, 0, 4));
    }

    @Test
    public void testSimpleLookahead() throws Exception {
        RePattern exp = HsrePattern.compile("^a(?=bc$)", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        ReMatcher matcher = exp.matcher("abc");
        assertThat(matcher.find(), is(true));
        assertThat(matcher.groupCount(), equalTo(0));
        assertThat(matcher, groupIs(0, 0, 1));
    }

    @Test
    public void testNonGreedy() throws Exception {
        // ? is advanced?
        RePattern exp = HsrePattern.compile("3z*?", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        ReMatcher matcher = exp.matcher("123zzz456");
        assertThat(matcher.find(), is(true));
        assertThat(0, equalTo(matcher.groupCount()));
        assertThat(matcher, groupIs(0, 2, 3));
    }


    //assertThat("b", not(matches("a", PatternFlags.BASIC)));

    @Test
    public void testBackreference() throws Exception {
        String[] ana = new String[]{"a"};
        assertThat("aa", matches("([ab])\\1", ana, PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("ab", not(matches("([ab])\\1", PatternFlags.ADVANCED, PatternFlags.EXPANDED)));
        assertThat("aa", matches("\\(a\\)\\1", ana, PatternFlags.BASIC, PatternFlags.BASIC));
    }

}
