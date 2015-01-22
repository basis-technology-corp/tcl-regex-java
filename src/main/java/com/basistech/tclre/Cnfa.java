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

import java.io.Serializable;

/**
 * Compacted (runtime) NFA.
 */
class Cnfa implements Serializable {
    static final long serialVersionUID = 1L;
    final int ncolors;        /* number of colors */
    final boolean hasLacons;
    final int pre;        /* setup state number */
    final int post;       /* teardown state number */
    final short[] bos;     /* colors, if any, assigned to BOS and BOL */
    final short[] eos;     /* colors, if any, assigned to EOS and EOL */
    final long[] arcs;
    // each state is an index of an arc.
    final int[] states;


    Cnfa(int ncolors, boolean hasLacons, int pre, int post, short[] bos, short[] eos, long[] arcs, int[] states) {
        this.ncolors = ncolors;
        this.hasLacons = hasLacons;
        this.pre = pre;
        this.post = post;
        this.bos = bos;
        this.eos = eos;
        this.arcs = arcs;
        this.states = states;
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
}
