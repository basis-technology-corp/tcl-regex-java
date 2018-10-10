/*
* Copyright 2014 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.basistech.tclre;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import java.util.BitSet;

/**
 * Runtime state set.
 * regexec.c
 */
class StateSet {
    BitSet states; // states -- we would really like this to be final & immutable.
    boolean poststate;
    boolean noprogress;
    Arcp ins;
    StateSet[] outs;
    Arcp[] inchain;

    /* 'privatized' to facilitate some debugging. */
    private int lastseen; // index of last entered on arrival here

    StateSet(int nstates, int ncolors) {
        states = new BitSet(nstates);
        // if colors are sparse these will need to be otherwise.
        outs = new StateSet[ncolors];
        inchain = new Arcp[ncolors];
        lastseen = -1;
    }

    StateSet(BitSet states, int ncolors) {
        this.states = states;
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
        return MoreObjects.toStringHelper(this)
                .add("states", states)
                .add("noprogress", noprogress)
                .add("poststate", poststate)
                .add("lastseen", lastseen)
                .toString();
    }
}
