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

import com.google.common.base.Objects;

/**
* Arc
*/
class Arc {
    static final int ARCFREE = 0;
    int type;
    short co;
    State from;	/* where it's from (and contained within) */
    State to;	/* where it's to */
    Arc outchain;	/* *from's outs chain or free chain */
    //define	freechain	outchain
    Arc inchain;	/* *to's ins chain */
    Arc colorchain;	/* color's arc chain */

    /** is an arc colored, and hence on a color chain? */
    boolean colored() {
        return type == Compiler.PLAIN || type == Compiler.AHEAD || type == Compiler.BEHIND;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("co", co)
                .add("from", from)
                .add("to", to)
                .add("outchain", outchain)
                .add("inchain", inchain)
                .add("colorchain", colorchain)
                .toString();
    }
}
