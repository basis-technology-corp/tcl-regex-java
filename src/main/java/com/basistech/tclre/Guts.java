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


import java.util.List;
import com.google.common.collect.Lists;

/**
 * Data structures derived from regguts.h.
 * Note that this is built for 16-bit chars.
 */
class Guts {
    int cflags;     /* copy of compile flags */
    long info;      /* copy of re_info */
    int nsub;       /* copy of re_nsub */
    Subre tree;
    Cnfa search;    /* for fast preliminary search */
    int ntree;
    ColorMap cm;
    SubstringComparator compare;

    List<Subre> lacons; /* lookahead-constraint vector */

    Guts() {
        lacons = Lists.newArrayList();
    }
}
