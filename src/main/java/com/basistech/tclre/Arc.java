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

import com.google.common.base.Objects;

/**
 * Arc
 */
class Arc {
    final int type;
    short co;
    final State from; /* where it's from (and contained within) */
    final State to;   /* where it's to */
    Arc outchain;   /* *from's outs chain or free chain */
    //define    freechain   outchain
    Arc inchain;    /* *to's ins chain */
    Arc colorchain; /* color's arc chain */

    Arc(int type, short co, State from, State to) {
        this.type = type;
        this.co = co;
        this.from = from;
        this.to = to;
    }

    /**
     * is an arc colored, and hence on a color chain?
     */
    boolean colored() {
        return type == Compiler.PLAIN || type == Compiler.AHEAD || type == Compiler.BEHIND;
    }

    void setColor(short co) {
        this.co = co;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("type", type)
                .add("co", co)
                .add("from", from)
                .add("to", to)
                .toString();
    }
}
