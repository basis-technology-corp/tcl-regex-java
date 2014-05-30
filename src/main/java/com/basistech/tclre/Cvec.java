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
 * Interface definitions for locale-interface functions in locale.c.
 * Multi-character collating elements (MCCEs) cause most of the trouble.
 */
class Cvec {
    static final int MAXMCCE = 2;
    int nchrs;		/* number of chrs */
    int chrspace;		/* number of chrs possible */
    char[] chrs;		/* pointer to vector of chrs */
    int nranges;		/* number of ranges (chr pairs) */
    int rangespace;		/* number of chrs possible */
    char[] ranges;		/* pointer to vector of chr pairs */
    int nmcces;		/* number of MCCEs */
    int mccespace;		/* number of MCCEs possible */
    int nmccechrs;		/* number of chrs used for MCCEs */
    char[][]mcces;		/* pointers to 0-terminated MCCEs */

    boolean haschr(char c) {
        return false;
    }

}  /* and both batches of chrs are on the end */
