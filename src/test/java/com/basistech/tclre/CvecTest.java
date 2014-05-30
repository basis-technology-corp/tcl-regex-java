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

import org.junit.Assert;
import org.junit.Test;

/**
 * The mcess angle here is probably wrong and so is not yet tested.
 */
public class CvecTest extends Assert {

    @Test
    public void cvec() {
        Cvec cvec = new Cvec(100, 100, 100); // numbers don't matter much
        assertFalse(cvec.haschr('a'));
        cvec.addchr('a');
        assertTrue(cvec.haschr('a'));
        cvec.addrange('a', 'z');
        assertTrue(cvec.haschr('q'));
        assertTrue(cvec.haschr('z'));
        cvec.addrange('1', '9');
        assertTrue(cvec.haschr('q'));
        assertTrue(cvec.haschr('z'));
        assertTrue(cvec.haschr('6'));
        cvec.clearcvec();
        assertFalse(cvec.haschr('a'));
        assertFalse(cvec.haschr('q'));
        assertFalse(cvec.haschr('4'));
    }
}
