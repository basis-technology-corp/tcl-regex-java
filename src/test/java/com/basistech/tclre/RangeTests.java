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
 * More examples of ranges to exercise the color map
 * TODO: figure out what case causes okcolors to promote (and thus exercise the 'free' code).
 */
public class RangeTests extends Utils {

    @Test
    public void testKitchenSink() throws Exception {
        HsrePattern exp = HsrePattern.compile("[^a][\u4e00-\uf000][[:upper:]][^\ufeff][\u4e00-\u4e10]b.c.d",
                PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("Q\u4e01A$\u4e09bGcHd", matches(exp));
    }



    @Test
    public void testNegativeRange() throws Exception {
        HsrePattern exp = HsrePattern.compile("[^a]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("Q", matches(exp));
        assertThat("a", not(matches(exp)));
    }

    @Test
    public void testUnicodeRange() throws Exception {
        HsrePattern exp = HsrePattern.compile("[\u4e00-\uf000]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("\u4e01", matches(exp));
    }

    @Test
    public void testNotBom() throws Exception {
        HsrePattern exp = HsrePattern.compile("[^\ufeff]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("$", matches(exp));
    }

    @Test
    public void testUpper() throws Exception {
        HsrePattern exp = HsrePattern.compile("[[:upper:]]", PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        assertThat("A", matches(exp));
    }
}
