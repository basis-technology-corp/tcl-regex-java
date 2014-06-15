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

import org.junit.Test;

import static com.basistech.tclre.Utils.Matches.matches;
import static org.hamcrest.CoreMatchers.not;

/**
 * Word boundary cases
 */
public class BoundaryTests extends Utils {

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
