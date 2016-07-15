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

/**
 * Mutable builder of immutable CNFA objects.
 */
class CnfaBuilder {
    int ncolors;        /* number of colors */
    boolean hasLacons;
    int pre;        /* setup state number */
    int post;       /* teardown state number */
    short[] bos;     /* colors, if any, assigned to BOS and BOL */
    short[] eos;     /* colors, if any, assigned to EOS and EOL */
    long[] arcs;
    // each state is an index of an arc.
    int[] states;


    CnfaBuilder(int nstates,
         int narcs,
         int preNo,
         int postNo,
         short[] bos,
         short[] eos,
         int maxcolors,
         boolean hasLacons) {

        this.pre = preNo;
        this.post = postNo;
        this.bos = bos;
        this.eos = eos;
        this.ncolors = maxcolors;
        this.hasLacons = hasLacons;

        this.arcs = new long[narcs];
        this.states = new int[nstates];
    }

    Cnfa build() {
        return new Cnfa(ncolors, hasLacons, pre, post, bos, eos, arcs, states);
    }

    void setState(int index, int arcIndex) {
        states[index] = arcIndex;
    }

    void setArc(int index, long arcValue) {
        arcs[index] = arcValue;
    }

    static long packCarc(short color, int targetState) {
        return ((long)color << 32) | targetState;
    }


    /**
     * carcsort - sort compacted-NFA arcs by color
     * Really dumb algorithm, but if the list is long enough for that to matter,
     * you're in real trouble anyway.
     */
    void carcsort(int first, int last) {
        int p;
        int q;
        long tmp;

        if (last - first <= 1) {
            return;
        }

        for (p = first; p <= last; p++) {
            for (q = p; q <= last; q++) {
                short pco = Cnfa.carcColor(arcs[p]);
                short qco = Cnfa.carcColor(arcs[q]);
                int pto = Cnfa.carcTarget(arcs[p]);
                int qto = Cnfa.carcTarget(arcs[q]);
                if (pco > qco || (pco == qco && pto > qto)) {
                    assert p != q;
                    tmp = arcs[p];
                    arcs[p] = arcs[q];
                    arcs[q] = tmp;
                }
            }
        }
    }
}
