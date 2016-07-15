/*
* Copyright 2014 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.basistech.tclre;

import java.util.EnumSet;

import org.junit.Test;

import static com.basistech.tclre.Utils.Matches.matches;
import static com.basistech.tclre.Utils.MatcherMatches.groupIs;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * some 'pons asinorum' tests to see if the runtime works at all.
 */
public class SimpleTest extends Utils {

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
        assertThat("qzbaabzq", matches("([ab])\\1", ana, PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("qzbaa", matches("([ab]*?|x)\\1$", ana, PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("qzbxxa", matches("([ab]*?|x)\\1$", PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("qacacq", matches("([ab]*?c)\\1", new String[]{"ac"}, PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("qacadq", not(matches("([ab]*?c)\\1", PatternFlags.ADVANCED, PatternFlags.EXPANDED)));
        assertThat("ab", not(matches("([ab])\\1", PatternFlags.ADVANCED, PatternFlags.EXPANDED)));
        assertThat("aa", matches("\\(a\\)\\1", ana, PatternFlags.BASIC, PatternFlags.BASIC));
        assertThat("qaaaaaaaaz", matches("q(a{0,4})\\1z", PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("qz", matches("q(a{0,4})\\1z", PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("cc", matches("(a|b|c)\\1", PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertThat("dd", not(matches("(a|b|c)\\1", PatternFlags.ADVANCED, PatternFlags.EXPANDED)));
    }

}
