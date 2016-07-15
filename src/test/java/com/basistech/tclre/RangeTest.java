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

import org.junit.Test;

import static com.basistech.tclre.Utils.Matches.matches;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * More examples of ranges to exercise the color map
 * TODO: figure out what case causes okcolors to promote (and thus exercise the 'free' code).
 */
public class RangeTest extends Utils {

    @Test
    public void testKitchenSink() throws Exception {
        RePattern exp = HsrePattern.compile("[^a][\u4e00-\uf000][[:upper:]][^\ufeff][\u4e00-\u4e10]b.c.d",
                PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("Q\u4e01A$\u4e09bGcHd", matches(exp));
    }

    @Test
    public void testNegativeRange() throws Exception {
        RePattern exp = HsrePattern.compile("[^a]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("Q", matches(exp));
        assertThat("a", not(matches(exp)));
    }

    @Test
    public void testUnicodeRange() throws Exception {
        RePattern exp = HsrePattern.compile("[\u4e00-\uf000]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("\u4e01", matches(exp));
    }

    @Test
    public void testNotBom() throws Exception {
        RePattern exp = HsrePattern.compile("[^\ufeff]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("$", matches(exp));
    }

    @Test
    public void testUpper() throws Exception {
        RePattern exp = HsrePattern.compile("[[:upper:]]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("A", matches(exp));
    }

    @Test
    public void rangeSplit() throws Exception {
        RePattern exp = HsrePattern.compile("[a-z]ab", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("zab", matches(exp));
        // make two ranges, then split both.
        exp = HsrePattern.compile("[a-f][g-z][e-h]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("bif", matches(exp));
        assertThat("bie", matches(exp));
        assertThat("fie", matches(exp));
        assertThat("azh", matches(exp));
    }
}
