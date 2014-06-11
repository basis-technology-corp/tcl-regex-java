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
 * The flags that disable ^/$
 */
public class AnchorFlagTests extends Utils {

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
}
