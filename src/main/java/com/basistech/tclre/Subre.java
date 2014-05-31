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

import com.google.common.base.Objects;

/**
* Created by benson on 5/29/14.
*/ /*
* subexpression tree
*/
class Subre {
    byte op;		/* '|', '.' (concat), 'b' (backref), '(', '=' */
    byte flags;
    static final int	LONGER = 01;	/* prefers longer match */
    static final int	SHORTER	= 02;	/* prefers shorter match */
    static final int	MIXED = 04;	/* mixed preference below */
    static final int	CAP = 010; /* capturing parens below */
    static final int	BACKR = 020;	/* back reference below */
    static final int	INUSE = 0100;	/* in use in final tree */
    static final int	LOCAL = 03;	/* bits which may not propagate up */

    /** LONGER -> MIXED */
    static int lmix(int f) {
        return f << 2;
    }

    /** SHORTER -> MIXED */
    static int smix(int f) {
        return f << 1;
    }

    static int up(int f) {
        return (f & ~LOCAL) | (lmix(f) & smix(f) & MIXED);
    }

    static boolean messy(int f) {
        return 0 != (f & (MIXED|CAP|BACKR));
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

    short retry;		/* index into retry memory */
    int subno;		/* subexpression number (for 'b' and '(') */
    short min;		/* min repetitions, for backref only */
    short max;		/* max repetitions, for backref only */
    Subre left;	/* left child, if any (also freelist chain) */
    Subre right;	/* right child, if any */
    State begin;	/* outarcs from here... */
    State end;	/* ...ending in inarcs here */
    Cnfa cnfa;	/* compacted NFA, if any */
    Subre chain;	/* for bookkeeping and error cleanup */

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("op", op)
                .add("flags", flags)
                .add("retry", retry)
                .add("subno", subno)
                .add("min", min)
                .add("max", max)
                .toString();
    }
}
