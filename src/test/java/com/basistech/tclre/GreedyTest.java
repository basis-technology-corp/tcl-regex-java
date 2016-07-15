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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;

/**
 * Tests that greedy matching works as expected.
 */
public class GreedyTest extends Utils {
    @Test
    public void greedy() throws Exception {
        RePattern pattern = HsrePattern.compile("\\s*foo\\s*", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("Monday  foo  Tuesday");
        assertTrue(matcher.find());
        assertThat(matcher.start(), is(equalTo(6)));
        assertThat(matcher.end(), is(equalTo(13)));
    }

    @Test
    public void greedyLookingAt() throws Exception {
        RePattern pattern = HsrePattern.compile("\\s*foo\\s*", PatternFlags.ADVANCED);
        ReMatcher matcher = pattern.matcher("  foo  ");
        assertTrue(matcher.matches());
    }
}
