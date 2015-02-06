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
