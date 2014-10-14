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

/**
 * patterns compiled with case-insensitivity.
 * TODO: tricky cases like the Turkish I.
 */
public class SingleCaseTests extends Utils {
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
}
