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
 * Color descriptor.
 */
class ColorDesc {
    static final int FREECOL = 1; // currently free
    static final int PSEUDO = 2;  // pseudocolor, no real chars

    int free; // free chain.
    short sub; // open subcolor (if any); free chain ptr */
    Arc arcs; /* color chain; linked list of arcs. */
    int flags;

    ColorMap.Tree block; /* block of color if any */
    private int nchars; // number of chars of this color

    ColorDesc() {
        sub = Constants.NOSUB;
        free = -1;
    }

    void reset() {
        nchars = 0;
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

    void setNChars(int nchars) {
        this.nchars = nchars;
    }

    int getNChars() {
        return nchars;
    }

    void incrementNChars(int incr) {
        this.nchars += incr;
    }
}
