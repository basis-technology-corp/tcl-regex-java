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

import com.google.common.base.Objects;

/**
 * State object in runtime.
 */
class State {
    static final int FREESTATE = -1;
    int no;
    int flag;       /* marks special states */
    int nins;       /* number of inarcs */
    Arc ins;    /* chain of inarcs */
    int nouts;      /* number of outarcs */
    Arc outs;   /* chain of outarcs */
    State tmp;  /* temporary for traversal algorithms */
    State next; /* chain for traversing all */
    State prev; /* back chain */

    Arc findarc(int type, short co) {
        for (Arc a = outs; a != null; a = a.outchain) {
            if (a.type == type && a.co == co) {
                return a;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("no", no)
                .add("flag", flag)
                .toString();
    }
}
