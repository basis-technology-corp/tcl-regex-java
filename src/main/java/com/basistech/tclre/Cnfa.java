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

        if (last - first <= 1)
            return;

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
