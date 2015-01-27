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
import java.util.List;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

/**
 * The bits and pieces that make up a runnable expression. This is immutable.
 */
class Guts implements Serializable {
    static final long serialVersionUID = 1L;

    final int cflags;     /* copy of compile flags */
    final long info;      /* copy of re_info */
    final int nsub;       /* copy of re_nsub */
    final RuntimeSubexpression tree;
    final Cnfa search;    /* for fast preliminary search */
    final int ntree;
    final RuntimeColorMap cm;
    final SubstringComparator compare;

    private List<RuntimeSubexpression> lookaheadConstraintMachines;

    public Guts(int cflags, long info, int nsub, RuntimeSubexpression tree, Cnfa search, int ntree, ColorMap cm, SubstringComparator compare, List<Subre> lacons) {
        this.cflags = cflags;
        this.info = info;
        this.nsub = nsub;
        this.tree = tree;
        this.search = search;
        this.ntree = ntree;
        // create the sort of color map that we can serialize and share.
        this.cm = new RuntimeColorMap(cm.getMap());
        this.compare = compare;
        if (lacons != null) {
            lookaheadConstraintMachines = Lists.newArrayList();
            for (Subre subre : lacons) {
                if (subre == null) {
                    lookaheadConstraintMachines.add(new RuntimeSubexpression());
                } else {
                    lookaheadConstraintMachines.add(new RuntimeSubexpression(subre));
                }
            }
        }
    }

    RuntimeSubexpression lookaheadConstraintMachine(int index) {
        return lookaheadConstraintMachines.get(index);
    }
}
