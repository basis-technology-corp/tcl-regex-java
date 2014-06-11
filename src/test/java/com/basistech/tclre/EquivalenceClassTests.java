/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2014 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclre;

import java.util.EnumSet;

import org.junit.Ignore;
import org.junit.Test;

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
        RegExp exp = compile("[[=a=]]", EnumSet.of(PatternFlags.ADVANCED, PatternFlags.EXPANDED));
        assertTrue(doMatch(exp, "a"));
        // C didn't implement this, and neither do we.
        assertFalse(doMatch(exp, "A"));
    }
}
