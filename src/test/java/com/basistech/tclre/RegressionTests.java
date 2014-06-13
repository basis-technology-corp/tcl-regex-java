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

/**
 * Home for tests that result from problems seen 'in the wild'.
 */
public class RegressionTests extends Utils {

    @Test
    public void nestedNestAssertion() throws Exception {
        //String boom = "(?i)\\m(?:(?:[\\+\\-]?\\d{1,3}(?:,?\\d{3})*(?:\\.\\d+)?|\\d{1,3}(?:\\.?\\d{3})*(?:,\\d+)?)\\s?-?\\s?(?:dozens?|hundreds?|thousands?|millions?|billions?|trillions?)?|(?:an?)\\s+(?:dozens?|hundreds?|thousands?|millions?|billions?|trillions?)?|(?:(?:zero|one|first|two|second|three|thirds?|four|fourths?|five|fifths?|six|seven(?:th(?:s)?)?|eight|eighths?|nine|ninths?|tens?|tenths?|eleven|twelve|twelfths?|thirteen|fourteen|fifteen|sixteen(?:th(?:s)?)?|seventeen|eighteen|nineteen|(?:twent|thirt|fort|fift|sixt|sevent|eight|ninet)(?:y|ieths?))|(?:an?)\\s+(?:dozens?|hundreds?|thousands?|millions?|billions?|trillions?))(?:\\s?(?:and|-)?\\s?(?:(?:zero|one|first|two|second|three|thirds?|four|fourths?|five|fifths?|six|seven(?:th(?:s)?)?|eight|eighths?|nine|ninths?|tens?|tenths?|eleven|twelve|twelfths?|thirteen|fourteen|fifteen|sixteen(?:th(?:s)?)?|seventeen|eighteen|nineteen|(?:twent|thirt|fort|fift|sixt|sevent|eight|ninet)(?:y|ieths?))|(?:dozens?|hundreds?|thousands?|millions?|billions?|trillions?),?))*)";
        String boom = "(?i)\\m(?:(?:[\\+\\-]?\\d{1,3}))";
        HsrePattern.compile(boom, PatternFlags.ADVANCED);
    }

}
