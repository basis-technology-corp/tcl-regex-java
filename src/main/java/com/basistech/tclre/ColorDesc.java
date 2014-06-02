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
 * Color descriptor.
 */
class ColorDesc {
    static final int FREECOL = 1; // currently free
    static final int PSEUDO = 2;  // pseudocolor, no real chars

    int nchrs; // number of chars of this color

    //TODO: sub puns a slot for two things, is this a good idea?
    short sub; // open subcolor (if any); free chain ptr */
    Arc arcs; /* color chain; linked list of arcs. */
    int flags;

    ColorMap.Tree block; /* block of color if any */

    ColorDesc() {
        sub = Constants.NOSUB;
    }

    void reset() {
        nchrs = 0;
        sub = Constants.NOSUB;
        flags = 0;
        arcs = null;
        block = null;
    }

    void markFree() {
        flags = FREECOL;
        block = null; // allow GC to take it.
    }

    boolean unusedColor() {
        return (flags & FREECOL) != 0;
    }

    boolean pseudo() {
        return (flags & PSEUDO) != 0;
    }
}
