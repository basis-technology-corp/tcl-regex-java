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
        HsrePattern exp = HsrePattern.compile("a|b", PatternFlags.ADVANCED, PatternFlags.ICASE);
        assertThat("a", matches(exp));
        assertThat("A", matches(exp));
        assertThat("b", matches(exp));
        assertThat("B", matches(exp));
        assertThat("c", not(matches(exp)));
        assertThat("C", not(matches(exp)));

    }

    @Test
    public void testRange() throws Exception {
        HsrePattern exp = HsrePattern.compile("[a-z]", PatternFlags.ADVANCED, PatternFlags.ICASE);
        assertThat("a", matches(exp));
        assertThat("A", matches(exp));
        assertThat("q", matches(exp));
        assertThat("Q", matches(exp));
        assertThat("$", not(matches(exp)));
    }
}
