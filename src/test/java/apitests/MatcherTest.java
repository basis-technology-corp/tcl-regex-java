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

package apitests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.basistech.tclre.HsrePattern;
import com.basistech.tclre.PatternFlags;
import com.basistech.tclre.ReMatcher;
import com.basistech.tclre.RePattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of misc apis in HsreMatcher
 */
public class MatcherTest extends Assert {

    @Test
    public void jreCheck() throws Exception {
        Pattern p = Pattern.compile("A");
        Matcher m = p.matcher("BAC");
        m.region(1, 2);
        assertTrue(m.find());
        assertEquals(1, m.start()); // offsets don't reflect region.
        // how about find offset.
        assertTrue(m.find(1)); // if not, this will be true.
        assertFalse(m.find(2)); // if not, this will be true.

    }

    @Test
    public void reset() throws Exception {
        RePattern pattern = HsrePattern.compile("a");
        ReMatcher matcher = pattern.matcher("a");
        assertTrue(matcher.find());
        ReMatcher reset = matcher.reset("b");
        assertSame(reset, matcher);
        assertFalse(matcher.find());

        matcher = pattern.matcher("1234a");
        matcher.region(3, 5);
        assertTrue(matcher.find());
        assertEquals(4, matcher.start());
        // check that reset(charseq) set up the region correctly.
        matcher.reset("a23");
        assertTrue(matcher.find());
        assertEquals(0, matcher.start());
    }

    @Test
    public void ranges() throws Exception {
        RePattern pattern = HsrePattern.compile("a");
        ReMatcher matcher = pattern.matcher("abab");
        assertTrue(matcher.find());
        matcher.region(1, 2);
        assertFalse(matcher.find());
        matcher.reset();
        assertTrue(matcher.find());
        matcher.region(2, 3);
        assertTrue(matcher.find());
        assertEquals(2, matcher.start());
        assertEquals(3, matcher.end());
    }

    @Test
    public void matches() throws Exception {
        RePattern pattern = HsrePattern.compile("ab");
        ReMatcher matcher = pattern.matcher("abab");
        assertFalse(matcher.matches());
        matcher = pattern.matcher("ab");
        assertTrue(matcher.matches());

        matcher = pattern.matcher("abab");
        matcher.region(0, 2);
        assertTrue(matcher.matches());
    }

    @Test
    public void lookingAt() throws Exception {
        RePattern pattern = HsrePattern.compile("ab");
        ReMatcher matcher = pattern.matcher("abcdefg");
        assertTrue(matcher.lookingAt());
        matcher.region(2, 7);
        assertFalse(matcher.lookingAt());
        matcher.reset("1234ab");
        assertFalse(matcher.lookingAt());
        matcher.region(4, 6);
        assertTrue(matcher.lookingAt());
    }

    @Test
    public void lookingAtPrefix() throws Exception {
        RePattern pattern = HsrePattern.compile("(?i)ab", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("abcdefg");
        assertTrue(matcher.lookingAt());
    }

    @Test
    public void lookingAtAREDirector() throws Exception {
        RePattern pattern = HsrePattern.compile("***:ab", PatternFlags.ADVANCED); // *** means ADVANCED
        ReMatcher matcher = pattern.matcher("abcdefg");
        assertTrue(matcher.lookingAt());
    }

    @Test
    public void findPrefix() throws Exception {
        RePattern pattern = HsrePattern.compile("(?i)ab", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("abcdefg");
        assertTrue(matcher.find());
    }

    @Test
    public void iteration() throws Exception {
        RePattern pattern = HsrePattern.compile("a");
        ReMatcher matcher = pattern.matcher("a.a.a.a.a.a");
        for (int x = 0; x < 6; x++) {
            assertTrue(matcher.find());
            assertEquals("start for iteration " + x, x * 2, matcher.start());
            assertEquals("end for iteration " + x, (x * 2) + 1, matcher.end());
        }
    }

    /*
    adjacencyRule with "^\s{0,5}"
adjacencyLength = 0

then, matchExact is called:

matchExact(null, buffer, offset, 0) <== 0 length
     */

    @Test
    public void zeroLengthInput() throws Exception {
        RePattern pattern = HsrePattern.compile("^\\s{0,5}");
        ReMatcher matcher = pattern.matcher("");
        char[] input = new char[10];
        matcher.reset(new String(input, 5, 5));
        matcher.matches();
    }

    /**
     *
     2
     管辖，公司
     \s{0,5}
     */
    @Test
    public void crashSpacesRangeChinese() throws Exception {
        RePattern pattern = HsrePattern.compile("\\s{0,5}", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("");
        matcher.reset(new String("管辖，公司".toCharArray(), 0, 5)).find();
        matcher.reset(new String("管辖，公司".toCharArray(), 0, 5)).region(0, 5).matches();
    }

}
