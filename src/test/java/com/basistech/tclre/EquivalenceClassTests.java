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
 * Let's imagine that both of the characters A and a fall at the same place in the collating sequence;
 * they belong to the same equivalence class. In that case, both of the bracket expressions
 * [[=A=]b] and [[=a=]b] are equivalent to writing [Aab]. As another example, if o and ô are
 * members of an equivalence class, then all of the bracket expressions [[=o=]], [[=ô=]], and
 * [oô] match those same two characters.
 */
public class EquivalenceClassTests extends Utils {

    @Test
    public void testSimpleEqv() throws Exception {
        RePattern exp = HsrePattern.compile("[[=a=]]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("a", matches(exp));
        // C didn't implement this, and neither do we.
        assertThat("A", not(matches(exp)));
    }
}
