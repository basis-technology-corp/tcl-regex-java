/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2015 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclre;

import org.junit.Test;

import static com.basistech.tclre.Utils.Matches.matches;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
}
