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


import com.google.common.collect.Lists;

import java.util.Comparator;
import java.util.List;

/**
 * Data structures derived from regguts.h.
 * Note that this is built for 16-bit chars.
 */
class Guts {
    int cflags;		/* copy of compile flags */
    long info;		/* copy of re_info */
    int nsub;		/* copy of re_nsub */
    Subre tree;
    Cnfa search;	/* for fast preliminary search */
    int ntree;
    ColorMap cmap;
    // see guava support when it comes time to fill this in.
    Comparator<char[]> compare;

    List<Subre> lacons;	/* lookahead-constraint vector */

    Guts() {
        lacons = Lists.newArrayList();
    }

    // length of above is nlacons.
    int nlacons() {
        return lacons.size();
    }
}
