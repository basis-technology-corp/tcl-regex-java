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
 * Runtime state set.
 * regexec.c
 */
class StateSet {
    static final int STARTER = 1;
    static final int POSTSTATE = 2;
    static final int LOCKED = 4;
    static final int NOPROGRESS = 8;


    /* Using BitSet and it's hash/equals
     * is probably going to be slower than we want
     * assuming that we get this to work at all. */
    boolean[] states; // states -- we would really like this to be final & immutable.
    int flags;
    Arcp ins;
    StateSet[] outs;
    Arcp[] inchain;

    /* 'privatized' to facilitate some debugging. */
    private int lastseen; // index of last entered on arrival here

    StateSet(int nsets, int ncolors) {
        states = new boolean[nsets];
        // if colors are sparse these will need to be otherwise.
        outs = new StateSet[ncolors];
        inchain = new Arcp[ncolors];
        lastseen = -1;
    }

    int getLastSeen() {
        return lastseen;
    }

    void setLastSeen(int lastseen) {
        this.lastseen = lastseen;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("states", states)
                .add("flags", Integer.toHexString(flags))
                .add("lastseen", lastseen)
                .toString();
    }
}
