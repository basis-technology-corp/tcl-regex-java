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

package apitests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.basistech.tclre.HsrePattern;
import com.basistech.tclre.ReMatcher;
import com.basistech.tclre.RePattern;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests of misc apis in HsreMatcher
 */
public class MatcherTests extends Assert {

    @Test
    public void jreCheck() throws Exception {
        Pattern p = Pattern.compile("A");
        Matcher m = p.matcher("BAC");
        m.region(1, 2);
        assertTrue(m.find());
        assertEquals(1, m.start()); // offsets don't reflect region.
        // how about find offset.
        assertTrue(m.find(1)); // if not, this will be true.
        assertFalse(m.find(2)); // if not, this will be true.

    }

    @Test
    public void reset() throws Exception {
        RePattern pattern = HsrePattern.compile("a");
        ReMatcher matcher = pattern.matcher("a");
        assertTrue(matcher.find());
        ReMatcher reset = matcher.reset("b");
        assertSame(reset, matcher);
        assertFalse(matcher.find());
    }

    @Test
    public void ranges() throws Exception {
        RePattern pattern = HsrePattern.compile("a");
        ReMatcher matcher = pattern.matcher("abab");
        assertTrue(matcher.find());
        matcher.region(1, 2);
        assertFalse(matcher.find());
        matcher.reset();
        assertTrue(matcher.find());
        matcher.region(2, 3);
        assertTrue(matcher.find());
        assertEquals(2, matcher.start());
        assertEquals(3, matcher.end());
    }

}
