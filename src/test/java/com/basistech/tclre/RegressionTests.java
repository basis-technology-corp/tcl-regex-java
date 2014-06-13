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

/**
 * Home for tests that result from problems seen 'in the wild'.
 */
public class RegressionTests extends Utils {

    @Test
    public void nestedNestAssertion() throws Exception {
        // blew up with assert in lexnest.
        String boom = "(?i)\\m(?:(?:[\\+\\-]?\\d{1,3}))";
        HsrePattern.compile(boom, PatternFlags.ADVANCED);
    }

    @Test
    public void testConfusedBrackets() throws Exception {
        // blew up interpreting s\:- as a class name.
        String boom = "[\\s\\:-]";
        Compiler.compile(boom,  EnumSet.of(PatternFlags.ADVANCED));
    }
}
