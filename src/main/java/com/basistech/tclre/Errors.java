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
 * Error constant ints.
 */
final class Errors {
    static final int REG_OKAY = 0;  /* no errors detected */
    static final int REG_NOMATCH = 1;   /* failed to match */
    static final int REG_BADPAT = 2;    /* invalid regexp */
    static final int REG_ECOLLATE = 3;  /* invalid collating element */
    static final int REG_ECTYPE = 4;    /* invalid character class */
    static final int REG_EESCAPE = 5;   /* invalid escape \ sequence */
    static final int REG_ESUBREG = 6;   /* invalid backreference number */
    static final int REG_EBRACK = 7;    /* brackets [] not balanced */
    static final int REG_EPAREN = 8;    /* parentheses () not balanced */
    static final int REG_EBRACE = 9;    /* braces {} not balanced */
    static final int REG_BADBR = 10;    /* invalid repetition count(s) */
    static final int REG_ERANGE = 11;   /* invalid character range */
    static final int REG_ESPACE = 12;   /* out of memory */
    static final int REG_BADRPT = 13;   /* quantifier operand invalid */
    static final int REG_INVARG = 16;   /* invalid argument to regex function */
    static final int REG_MIXED = 17;    /* character widths of regex and string differ */
    static final int REG_BADOPT = 18;   /* invalid embedded option */
    /* two specials for debugging and testing */
    static final int REG_ATOI = 101;    /* convert error-code name to number */
    static final int REG_ITOA = 102;    /* convert error-code number to name */

    private Errors() {
        //
    }

}
