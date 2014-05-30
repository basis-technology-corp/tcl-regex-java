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

/**
 * Created by benson on 5/29/14.
 */
class Constants {
    static final int NOCELT = -1;
    static final int CHRBITS = 16;
    static final int CHR_MIN = 0;
    static final int CHR_MAX = 0xffff;
    static final int BYTBITS = 8;
    static final int BYTTAB = 1 << BYTBITS;
    static final int BYTMASK = BYTTAB - 1;
    static final int NBYTS = (CHRBITS + BYTBITS - 1) / BYTBITS;
    static final short COLORLESS = -1;
    static final short NOSUB = COLORLESS;
    static final short WHITE = 0;

    static int chr(int c) {
        return c - '0';
    }
}
