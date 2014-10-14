/*
 * Copyright 2014 Basis Technology Corp.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.basistech.tclre;

/**
 * Compacted (runtime) NFA.
 */
class Cnfa {
    static final int HASLACONS = 1;
    int nstates;        /* number of states */
    int ncolors;        /* number of colors */
    int flags;
    int pre;        /* setup state number */
    int post;       /* teardown state number */
    short[] bos = new short[2];     /* colors, if any, assigned to BOS and BOL */
    short[] eos = new short[2];     /* colors, if any, assigned to EOS and EOL */
    long[] arcs;
    // each state is an index of an arc.
    int[] states;


    Cnfa(int nstates,
         int narcs,
         int preNo,
         int postNo, short[] bos,
         short[] eos, int maxcolors, int flags) {

        this.pre = preNo;
        this.post = postNo;
        this.nstates = nstates;
        this.bos = bos;
        this.eos = eos;
        this.ncolors = maxcolors;
        this.flags = flags;

        this.arcs = new long[narcs];
        this.states = new int[nstates];
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

    static short carcColor(long packed) {
        return (short)(packed >>> 32);
    }

    static int carcTarget(long packed) {
        return (int)packed;
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
                short pco = carcColor(arcs[p]);
                short qco = carcColor(arcs[q]);
                int pto = carcTarget(arcs[p]);
                int qto = carcTarget(arcs[q]);
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
