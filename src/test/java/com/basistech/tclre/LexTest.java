/*
 * Copyright 2014 Basis Technology Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.basistech.tclre;

import org.junit.Test;

import static com.basistech.tclre.Utils.Matches.matches;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test for the lexer.
 */
public class LexTest extends Utils{
    private void assertCatchCompileTime(String regex) {
        try {
            HsrePattern.compile(regex, PatternFlags.ADVANCED);
            assertTrue(false);  /* should throw! */
        } catch (RegexException ree) {
            assertTrue(true);
        }
    }

    @Test
    public void testLex() throws Exception {
        RePattern exp = HsrePattern.compile("a.*+[bc]d$+", PatternFlags.QUOTE);
        assertThat("a.*+[bc]d$+", matches(exp));
        assertThat("a.*+[ef]d$+", not(matches(exp)));
        assertCatchCompileTime("***?kaboom");
        assertCatchCompileTime("***qkaboum");
        assertCatchCompileTime("(?z)");

        HsrePattern.compile("(?b)", PatternFlags.ADVANCED);
        exp = HsrePattern.compile("(?i)aaBB", PatternFlags.ADVANCED);
        assertThat("aAbB", matches(exp));
        exp = HsrePattern.compile("(?c)aaBB", PatternFlags.ADVANCED);
        assertThat("aaBB",matches(exp));
        assertThat("aAbB", not(matches(exp)));
        exp = HsrePattern.compile("a\\nb", PatternFlags.ADVANCED);
        assertThat("a\nb", matches(exp));
        exp = HsrePattern.compile("(?e)a\\nb", PatternFlags.ADVANCED);
        assertThat("a\nb", not(matches(exp))); /* ERE mode */
        exp = HsrePattern.compile("(?n)^bcd$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", matches(exp));
        exp = HsrePattern.compile("(?s)^bcd$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", not(matches(exp)));
        exp = HsrePattern.compile("(?s)^01.*fg$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", matches(exp));
        exp = HsrePattern.compile("(?p)^01.*fg$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", not(matches(exp)));
        exp = HsrePattern.compile("(?p)^.*\n.*\n.*fg$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", matches(exp));
        exp = HsrePattern.compile("(?q)^01.*fg$", PatternFlags.ADVANCED);
        assertThat("^01.*fg$", matches(exp)); /* quote */
        exp = HsrePattern.compile("(?x)abc #bar baz", PatternFlags.ADVANCED);
        assertThat("abcbarbaz", matches(exp));
        exp = HsrePattern.compile("(?t)abc #bar baz", PatternFlags.ADVANCED);
        assertThat("abcbarbaz", not(matches(exp)));
        exp = HsrePattern.compile("(?w).*", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", matches(exp));
        exp = HsrePattern.compile("(?w)^.*fg$", PatternFlags.ADVANCED);
        assertThat("01a\nbcd\nefg", matches(exp));
        assertCatchCompileTime("(?p");
        exp = HsrePattern.compile("[[.number-sign.]]");
        assertThat("#", matches(exp));
        assertThat("n", not(matches(exp)));
        exp = HsrePattern.compile("a{3,5}?", PatternFlags.ADVANCED); /*non-greedy*/
        assertThat("aaaa", matches(exp));
        assertCatchCompileTime("a[3,4");
        assertCatchCompileTime("a{3,4");
        assertCatchCompileTime("[[:digit:}");
        exp = HsrePattern.compile("a\\x123q", PatternFlags.ADVANCED);
        assertThat("a\u0123q", matches(exp));

        assertCatchCompileTime("a[\\");
        exp = HsrePattern.compile("a[\\d]", PatternFlags.ADVANCED);
        assertThat("a3", matches(exp));
        assertThat("ab",not(matches(exp)));
        exp = HsrePattern.compile("a[\\w]", PatternFlags.ADVANCED);
        assertThat("aQ", matches(exp));
        assertThat("a$", not(matches(exp)));
        assertCatchCompileTime("a[[");
        exp = HsrePattern.compile("a\\{3,5\\}", PatternFlags.BASIC);
        assertThat("aaaa", matches(exp));
        assertThat("aa", not(matches(exp)));
        exp = HsrePattern.compile("a[\\]b", PatternFlags.BASIC);
        assertThat("a\\b", matches(exp));
        assertCatchCompileTime("a{3,q}");
        assertCatchCompileTime("a[\\q]");
        HsrePattern.compile("[[q]]", PatternFlags.ADVANCED);
        /* not clear what this (above) means, but code special-cases it. */
        exp = HsrePattern.compile("a.+?b", PatternFlags.ADVANCED);
        assertThat("acwwdcdbwfefwb", matches(exp)); /* won't say which*/
        assertThat("ab", not(matches(exp)));
        exp = HsrePattern.compile("a.??b", PatternFlags.ADVANCED); /* meaning.?? */
        assertThat("ab", matches(exp)); /* won't say which*/
        exp = HsrePattern.compile("a{b", PatternFlags.EXPANDED); /* meaning.?? */
        assertThat("a{b", matches(exp));
        assertThat("a[b", not(matches(exp)));
        exp = HsrePattern.compile("a(?#foo)b", PatternFlags.ADVANCED);
        assertThat("ab", matches(exp));
        exp = HsrePattern.compile("a(?!foo)b", PatternFlags.ADVANCED);
        assertThat("ab", matches(exp));
        assertCatchCompileTime("a(?&)b");
        exp = HsrePattern.compile("[\\r][\\b][\\f][\\a][\\e][\\v][\\t]", PatternFlags.ADVANCED);
        assertThat("\r\b\f\007\033\u000b\t", matches(exp));

        assertCatchCompileTime("\\c");
        assertCatchCompileTime("\\");
        exp = HsrePattern.compile("\\cC\\B\\w\\D\\S\\W", PatternFlags.ADVANCED);
        assertThat("\u0003\\_!$@", matches(exp));

        exp = HsrePattern.compile("[\\uABCD][\\uEF89][\\U12345678]?", PatternFlags.ADVANCED);
        assertThat("\uABCD\uEF89", matches(exp));
        HsrePattern.compile(".*[[:<:]].*[[:>:]].*", PatternFlags.ADVANCED);
        /* TODO:This works against C++, but doesn't work here. It's deprecated, though. */
        // assertThat("^%*&^AbulBakr@#$#@$", matches(exp));

        exp = HsrePattern.compile("*a*b$a$", PatternFlags.BASIC);
        assertThat("*aaab$a", matches(exp));
        exp = HsrePattern.compile("^q^b$c$", PatternFlags.BASIC);
        assertThat("q^b$c", matches(exp));
        assertCatchCompileTime("(?b)\\");
        exp = HsrePattern.compile("\\(a|b\\)", PatternFlags.BASIC);
        assertThat("a|b", matches(exp, new String[]{"a|b"}));
        exp = HsrePattern.compile("[ab]", PatternFlags.BASIC);
        assertThat("a", matches(exp));
        exp = HsrePattern.compile("\\(.*\\)\\<\\(.*\\)\\>\\(.*\\)", PatternFlags.BASIC);
        /* Amazingly, this works in this form in BASIC */
        assertThat("^%*&^AbulBakr@#$#@$", matches(exp, new String[]{"^%*&^","AbulBakr", "@#$#@$"}));
    }
}