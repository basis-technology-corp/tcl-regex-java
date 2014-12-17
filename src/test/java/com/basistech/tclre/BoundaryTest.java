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

/**
 * Word boundary cases
 */
public class BoundaryTest extends Utils {

    @Test
    public void wordBegin() throws Exception {
        assertThat("Q a R", matches("\\ma", PatternFlags.ADVANCED));
        assertThat("Q aR", matches("\\ma", PatternFlags.ADVANCED));
        assertThat("QaR", not(matches("\\ma", PatternFlags.ADVANCED)));
    }

    @Test
    public void wordEnd() throws Exception {
        assertThat("Q a R", matches("a\\M", PatternFlags.ADVANCED));
        assertThat("Qa R", matches("a\\M", PatternFlags.ADVANCED));
        assertThat("QaR", not(matches("a\\M", PatternFlags.ADVANCED)));
    }

    @Test
    public void wordEitherEnd() throws Exception {
        assertThat("Q a R", matches("\\ya", PatternFlags.ADVANCED));
        assertThat("QaR", not(matches("\\ya", PatternFlags.ADVANCED)));

        assertThat("Q a R", not(matches("\\Ya", PatternFlags.ADVANCED)));
        assertThat("QaR", matches("\\Ya", PatternFlags.ADVANCED));
    }

    @Test
    public void stringStarts() throws Exception {
        assertThat("a123", matches("\\Aa", PatternFlags.ADVANCED));
        assertThat("ba123", not(matches("\\Aa", PatternFlags.ADVANCED)));
    }

    @Test
    public void stringEnds() throws Exception {
        assertThat("a123", matches("3\\Z", PatternFlags.ADVANCED));
        assertThat("a123b", not(matches("a\\Z", PatternFlags.ADVANCED)));
    }
}
