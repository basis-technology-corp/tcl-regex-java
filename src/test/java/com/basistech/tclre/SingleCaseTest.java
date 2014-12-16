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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * patterns compiled with case-insensitivity.
 * TODO: tricky cases like the Turkish I.
 */
public class SingleCaseTest extends Utils {
    @Test
    public void testAlternation() throws Exception {
        RePattern exp = HsrePattern.compile("a|b", PatternFlags.ADVANCED, PatternFlags.ICASE);
        assertThat("a", matches(exp));
        assertThat("A", matches(exp));
        assertThat("b", matches(exp));
        assertThat("B", matches(exp));
        assertThat("c", not(matches(exp)));
        assertThat("C", not(matches(exp)));

    }

    @Test
    public void testRange() throws Exception {
        RePattern exp = HsrePattern.compile("[a-z]", PatternFlags.ADVANCED, PatternFlags.ICASE);
        assertThat("a", matches(exp));
        assertThat("A", matches(exp));
        assertThat("q", matches(exp));
        assertThat("Q", matches(exp));
        assertThat("$", not(matches(exp)));
    }

    @Test
    public void testTEJ348() throws Exception {
        String patternStr = "(?i)\\m(?:(?:(?:(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)|(?:tues?|thu(?:rs?)?|fri)\\.?)|(?:mon|wed|sat|sun)\\.?)\\s*,?\\s+)?(?:(?:(?:january|february|july|september|october|november|december)|(?:april|june)\\.?)|(?:(?:August)\\.?)|(?:march|may)|(?:jan|feb|mar|apr|jun|jul|aug|sept?|oct|nov|dec)\\.?)(?:(?:\\s+the)?\\s+(?:0?[1-9]|[12]\\d|3[01])(?:th|nd|rd|st)?)(?:\\s*,?\\s*(?:\\d{4}))?\\M";
        RePattern pattern = HsrePattern.compile(patternStr, PatternFlags.ADVANCED);
        String str = "The event happened on january 3rd at 4PM on Wednesday.";
        ReMatcher matcher = pattern.matcher(str);
        matcher.region(0, 50);

        // We're testing a date/time pattern against the input string, so there's
        // a match in offsets 22-33 corresponding to "january 3rd"
        boolean res = matcher.lookingAt();


        assertFalse(res);

        matcher.region(9, str.length());
        assertFalse(matcher.lookingAt());

        matcher.region(22, str.length());
        assertTrue(matcher.lookingAt());
        assertEquals(22, matcher.start());
    }
}
