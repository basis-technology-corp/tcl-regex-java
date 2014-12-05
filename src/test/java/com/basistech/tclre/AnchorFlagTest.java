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

import java.util.EnumSet;

import org.junit.Test;
import static com.basistech.tclre.Utils.Matches.matches;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * The flags that disable ^/$
 */
public class AnchorFlagTest extends Utils {

    @Test
    public void testNotBol() throws Exception {
        assertThat("hello", not(matches("^hello", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.ICASE),
                EnumSet.of(ExecFlags.NOTBOL))));
        assertThat("hello", matches("^hello", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.ICASE),
                EnumSet.noneOf(ExecFlags.class)));
    }

    @Test
    public void testNotEol() throws Exception {
        assertThat("--hello", matches("hello$", PatternFlags.ADVANCED));
        assertThat("--hello", not(matches("hello$", EnumSet.of(PatternFlags.ADVANCED), EnumSet.of(ExecFlags.NOTEOL))));
    }

    /*
     * Note: com.basistech.tclre.Runtime.exec always starts by calling 'shortest', and never bothers with 'longest'
     * unless shortest finds a match. So we don't need code in 'longest' that enforces LOOKING_AT, and we don't need
     * a test that provokes the longest path with the flag.
     */
    @Test
    public void lookingAtSimple() throws Exception {
        // LOOKING_AT simulates a ^ at the start.
        assertThat("xhello", not(matches("hello", EnumSet.of(PatternFlags.ADVANCED), EnumSet.of(ExecFlags.LOOKING_AT))));
    }

    @Test
    public void lookingAtBackreference() throws Exception {
        String[] ana = new String[]{"a"};
        assertThat("xaa", matches("([ab])\\1", ana, EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED), EnumSet.of(ExecFlags.LOOKING_AT)));
    }
}
