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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * subexpression tree
 */
class Subre {
    static final int LONGER = 01;   /* prefers longer match */
    static final int SHORTER = 02;  /* prefers shorter match */
    static final int MIXED = 04;    /* mixed preference below */
    static final int CAP = 010; /* capturing parens below */
    static final int BACKR = 020;   /* back reference below */
    static final int INUSE = 0100;  /* in use in final tree */
    static final int LOCAL = 03;    /* bits which may not propagate up */
    char op;        /* '|', '.' (concat), 'b' (backref), '(', '=' */
    int flags;
    short retry;        /* index into retry memory */
    int subno;      /* subexpression number (for 'b' and '(') */
    short min;      /* min repetitions, for backref only */
    short max;      /* max repetitions, for backref only */
    Subre left; /* left child, if any (also freelist chain) */
    Subre right;    /* right child, if any */
    State begin;    /* outarcs from here... */
    State end;  /* ...ending in inarcs here */
    Cnfa cnfa;

    Subre(char op, int flags, State initState, State finalState) {

        // 0 is valid for lacons.
        assert  "\u0000|.b(=".indexOf(op) != -1 : "invalid op " + Integer.toHexString((int)op);

        this.op = op;
        this.flags = flags;
        min = 1;
        max = 1;
        begin = initState;
        end = finalState;
    }

    /**
     * LONGER -> MIXED
     */
    static int lmix(int f) {
        return f << 2;
    }

    /**
     * SHORTER -> MIXED
     */
    static int smix(int f) {
        return f << 1;
    }

    static int up(int f) {
        return (f & ~LOCAL) | (lmix(f) & smix(f) & MIXED);
    }

    static boolean messy(int f) {
        return 0 != (f & (MIXED | CAP | BACKR));
    }

    static int pref(int f) {
        return f & LOCAL;
    }

    static int pref2(int f1, int f2) {
        return pref(f1) != 0 ? pref(f1) : pref(f2);
    }

    static int combine(int f1, int f2) {
        return up(f1 | f2) | pref2(f1, f2);
    }

    String shortId() {
        String id;
        if (retry != 0) {
            id = Integer.toString(retry);
        } else {
            id = toString();
        }
        return id;
    }

    /**
     * dumpst - dump a subRE tree
     */
    String dumpst(boolean nfapresent) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s. `%c'", shortId(), op));

        if (0 != (flags & LONGER)) {
            sb.append(" longest");
        }
        if (0 != (flags & SHORTER)) {
            sb.append(" shortest");
        }
        if (0 != (flags & MIXED)) {
            sb.append(" hasmixed");
        }
        if (0 != (flags & CAP)) {
            sb.append(" hascapture");
        }
        if (0 != (flags & BACKR)) {
            sb.append(" hasbackref");
        }
        if (0 == (flags & INUSE)) {
            sb.append(" UNUSED");
        }
        if (subno != 0) {
            sb.append(String.format(" (#%d)", subno));
        }
        if (min != 1 || max != 1) {
            sb.append(String.format(" {%d,", min));
            if (max != Compiler.INFINITY) {
                sb.append(String.format("%d", max));
            }
            sb.append("}");
        }
        if (nfapresent) {
            sb.append(String.format(" %d-%d", begin.no, end.no));
        }
        if (left != null) {
            sb.append(String.format(" L:%s", left.toString()));
        }
        if (right != null) {
            sb.append(String.format(" R:%s", right.toString()));
        }

        sb.append("\n");
        if (left != null) {
            left.dumpst(nfapresent);
        }
        if (right != null) {
            right.dumpst(nfapresent);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("op", op)
                .add("flags", flags)
                .add("retry", retry)
                .add("subno", subno)
                .add("min", min)
                .add("max", max)
                .toString();
    }
}
