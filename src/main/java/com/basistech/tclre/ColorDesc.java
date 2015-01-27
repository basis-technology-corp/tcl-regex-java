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

    short sub; // open subcolor (if any); free chain ptr */
    Arc arcs; /* color chain; linked list of arcs. */
    private boolean pseudo;
    private int nchars; // number of chars of this color

    ColorDesc() {
        sub = Constants.NOSUB;
    }

    void markPseudo() {
        pseudo = true;
    }

    boolean pseudo() {
        return pseudo;
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
