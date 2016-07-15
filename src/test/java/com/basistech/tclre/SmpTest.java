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
import static org.junit.Assert.assertTrue;

/**
 * Characters outside the SMP (woo-woo)
 */
public class SmpTest {
    @Test
    public void testSingleCharMatch() throws Exception {
        assertThat("b\uD800\uDF80c", matches("b.c", PatternFlags.BASIC));
        assertThat("bxyc", not(matches("b.c", PatternFlags.BASIC)));
        assertThat("\uD800\uDF80b", matches(".b", PatternFlags.BASIC));
        assertThat("b\uD800\uDF80", matches("b.", PatternFlags.BASIC));
    }

    @Test
    public void smpPattern() throws Exception {
        assertThat("b", not(matches("\uD800\uDF80", PatternFlags.BASIC)));
        assertThat("b\uD800\uDF80", matches(".\uD800\uDF80", PatternFlags.BASIC));
        assertThat("\uD800\uDF80", matches("[\\U00010380-\\U0001039F]", PatternFlags.ADVANCED));
        assertThat("\uD800\uDF80", matches("[\uD800\uDF80-\uD800\uDF8F]", PatternFlags.ADVANCED));
    }

    @Test
    public void findCharClass() throws Exception {
        RePattern pattern = HsrePattern.compile("[\\U00010380]", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80");
        assertTrue(matcher.find());
    }

    @Test
    public void find() throws Exception {
        RePattern pattern = HsrePattern.compile("\uD800\uDF80", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80.\uD800\uDF80");
        assertTrue(matcher.find());
    }
}
