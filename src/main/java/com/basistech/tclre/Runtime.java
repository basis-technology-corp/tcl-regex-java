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

import java.util.List;

/**
 * Runtime matcher. Not called 'matcher' to save that
 * name for some presentable API.
 */
class Runtime {
    RegExp re;
    Guts g;
    int eflags;
    List<RegMatch> match;
    RegMatch details;
    int startIndex;
    int endIndex;
    char[] data;
    int[] mem; // backtracking.

    /**
     * exec - match regular expression
     */
    int exec(RegExp re, char[] data, int startIndex, int endIndex, int flags) {
        this.re = re;
        this.g = re.guts;

    /* sanity checks */
    /* setup */

        if (0 != (g.info & Flags.REG_UIMPOSSIBLE)) {
            return -1;
        }

        eflags = flags;

        this.data = data;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        match.add(null); // make room for 1.
        mem = new int[g.ntree];
       
    /* do it */
        assert g.tree != null;

        int st;
        if (0 != (g.info & Flags.REG_UBACKREF)) {
            st = cfind(g.tree.cnfa);
        } else {
            st = find(g.tree.cnfa);
        }
        mem =  null;
        return st;
    }

    /**
     * find - find a match for the main NFA (no-complications case)
     */
    int find(Cnfa cnfa) {
        Dfa s;
        Dfa d;
        int begin;
        int end = -1;
        int cold;
        int open;		/* open and close of range of possible starts */
        int close;
        boolean hitend;
        boolean shorter = 0 != (g.tree.flags & Subre.SHORTER);

    /* first, a shot with the search RE */
        s = new Dfa(this, g.search);
        int[] coldp = new int[1];
        close = s.shortest(startIndex, startIndex, endIndex, coldp, null);
        cold = coldp[0];


        if (0 != (g.cflags & Flags.REG_EXPECT)) {
            int dtstart;
            if (cold != -1) {
                dtstart = cold;
            } else {
                dtstart = endIndex;
            }
            details = new RegMatch(dtstart, endIndex);
        }
        if (close == -1)		/* not found */
            return Errors.REG_NOMATCH;
        if (match.size() == 0) {	/* found, don't need exact location */
            return Errors.REG_OKAY;
        }

    /* find starting point and match */
        assert cold != -1;
        open = cold;

        cold = -1;
        d = new Dfa(this, cnfa);
        for (begin = open; begin <= close; begin++) {
            boolean[] hitendp = new boolean[1];
            if (shorter) {
                end = d.shortest(begin, begin, endIndex, null, hitendp);
            } else {
                end = d.longest(begin, endIndex, hitendp);
            }
            hitend = hitendp[0];

            if (hitend && cold == -1) {
                cold = begin;
            }
            if (end != -1) {
                break;		/* NOTE BREAK OUT */
            }
        }
        assert end != -1;		/* search RE succeeded so loop should */

        /* and pin down details */
        match.set(0, new RegMatch(begin, end));

        if (0 != (g.cflags & Flags.REG_EXPECT)) {
            int dtstart;
            if (cold != -1) {
                dtstart = cold;
            } else {
                dtstart = endIndex;
            }
            details = new RegMatch(dtstart, endIndex);
        }

        if (match.size() == 1) { /* no need for submatches */
            return Errors.REG_OKAY;
        }

        return dissect(g.tree, begin, end);
    }


    /**
     * cfind - find a match for the main NFA (with complications)
     */
    int cfind(Cnfa cnfa) {
        Dfa s;
        Dfa d;
        int[] cold = new int[1];
        int ret;

        s = new Dfa(this, g.search);
        d = new Dfa(this, cnfa);

        ret = cfindloop(cnfa, d, s, cold);

        int dtstart;
        if (cold[0] != -1) {
            dtstart = cold[0];
        } else {
            dtstart = endIndex;
        }

        details = new RegMatch(dtstart, this.endIndex);
        return ret;
    }

    /**
     * cfindloop - the heart of cfind
     */
    int cfindloop(Cnfa cnfa, Dfa d, Dfa s, int[] coldp) {
        int begin;
        int end;
        int cold;
        int open;		/* open and close of range of possible starts */
        int close;
        int estart;
        int estop;
        int er;
        boolean shorter = 0 != (g.tree.flags & Subre.SHORTER);
        boolean hitend[] = new boolean[1];

        assert d != null && s != null;
        cold = -1;
        close = startIndex;
        do {
            int[] cold0 = new int[1];
            close = s.shortest(close, close, endIndex, cold0, null);
            cold = cold0[0];

            if (close == -1) {
                break;				/* NOTE BREAK */
            }
            assert cold != -1;
            open = cold;
            cold = -1;

            for (begin = open; begin <= close; begin++) {
                estart = begin;
                estop = endIndex;
                for (; ; ) {
                    if (shorter) {
                        end = d.shortest(begin, estart, estop, null, hitend);
                    } else {
                        end = d.longest(begin, estop, hitend);
                    }
                    if (hitend[0] && cold == -1) {
                        cold = begin;
                    }
                    if (end == -1) {
                        break;		/* NOTE BREAK OUT */
                    }

                    match.clear();

                    int maxsubno = getMaxSubno(g.tree, 0);
                    mem = new int[maxsubno + 1];
                    er = cdissect(g.tree, begin, end);
                    if (er == Errors.REG_OKAY) {
                        match.add(new RegMatch(begin, end));
                        coldp[0] = cold;
                        return Errors.REG_OKAY;
                    }
                    if (er != Errors.REG_NOMATCH) {
                        return er;
                    }
                    if ((shorter) ? end == estop : end == begin) {
                        /* no point in trying again */
                        coldp[0] = cold;
                        return Errors.REG_NOMATCH;
                    }
                    /* go around and try again */
                    if (shorter) {
                        estart = end + 1;
                    } else {
                        estop = end - 1;
                    }
                }
            }
        } while (close < endIndex);

        coldp[0] = cold;
        return Errors.REG_NOMATCH;
    }

    /**
     * subset - set any subexpression relevant to a successful subre
     */
    void subset(Subre sub, int begin, int end) {
        int n = sub.subno;

        assert n > 0;

        while (match.size() < (n + 1)) {
            match.add(null);
        }

        match.set(n, new RegMatch(begin, end));
    }

    /**
     * dissect - determine subexpression matches (uncomplicated case)
     */
    int dissect(Subre t, int begin, int end) {
        switch (t.op) {
        case '=':		/* terminal node */
            assert t.left == null && t.right == null;
            return Errors.REG_OKAY;	/* no action, parent did the work */

        case '|':		/* alternation */
            assert t.left != null;
            return altdissect(t, begin, end);

        case 'b':		/* back ref -- shouldn't be calling us! */
            throw new RuntimeException("impossible backref");


        case '.':		/* concatenation */
            assert t.left != null && t.right != null;
            return condissect(t, begin, end);

        case '(':		/* capturing */
            assert t.left != null && t.right == null;
            assert t.subno > 0;
            subset(t, begin, end);
            return dissect(t.left, begin, end);

        default:
            throw new RuntimeException("Impossible op");
        }
    }


    /**
     * condissect - determine concatenation subexpression matches (uncomplicated)
     */
    int condissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;
        int i;
        boolean shorter = (t.left.flags & Subre.SHORTER) != 0;
        int stop = (shorter) ? end : begin;

        assert t.op == '.';
        assert t.left != null && t.left.cnfa.nstates > 0;
        assert t.right != null && t.right.cnfa.nstates > 0;

        d = new Dfa(this, t.left.cnfa);
        d2 = new Dfa(this, t.right.cnfa);

    /* pick a tentative midpoint */
        if (shorter) {
            mid = d.shortest(begin, begin, end, null, null);
        } else {
            mid = d.longest(begin, end, null);
        }
        if (mid == -1) {
            throw new RuntimeException("Impossible mid.");
        }

    /* iterate until satisfaction or failure */
        while (d2.longest(mid, end, null) != end) {
        /* that midpoint didn't work, find a new one */
        if (mid == stop) {
            /* all possibilities exhausted! */
            throw new RuntimeException("no midpoint");
        }
        if (shorter) {
            mid = d.shortest(begin, mid + 1, end, null, null);
        } else {
            mid = d.longest(begin, mid - 1, null);
        }
        if (mid == -1) {
            throw new RuntimeException("Failed midpoint");
        }
    }

    /* satisfaction */
        i = dissect(t.left, begin, mid);
        if (i != Errors.REG_OKAY) {
            return i;
        }
        return dissect(t.right, mid, end);
    }

    /**
     * altdissect - determine alternative subexpression matches (uncomplicated)
     */
    int altdissect(Subre t, int begin, int end) {
        Dfa d;
        int i;

        assert t != null;
        assert t.op == '|';

        for (i = 0; t != null; t = t.right, i++) {
            assert(t.left != null && t.left.cnfa.nstates > 0);
            d = new Dfa(this, t.left.cnfa);
            if (d.longest(begin, end, null) == end) {
                return dissect(t.left, begin, end);
            }
        }
        throw new RuntimeException("none matched");
    }



    /**
     * cdissect - determine subexpression matches (with complications)
     * The retry memory stores the offset of the trial midpoint from begin,
     * plus 1 so that 0 uniquely means "clean slate".
     */
    int cdissect(Subre t, int begin, int end) {
        int er;

        assert t != null;

        switch (t.op) {
        case '=':		/* terminal node */
            assert t.left == null && t.right == null;
            return Errors.REG_OKAY;	/* no action, parent did the work */

        case '|':		/* alternation */
            assert (t.left != null);
            return caltdissect(t, begin, end);

        case 'b':		/* back ref -- shouldn't be calling us! */
            assert (t.left == null && t.right == null);
            return cbrdissect(t, begin, end);

        case '.':		/* concatenation */
            assert (t.left != null && t.right != null);
            return ccondissect(t, begin, end);

        case '(':		/* capturing */
            assert (t.left != null && t.right == null);
            assert (t.subno > 0);
            er = cdissect(t.left, begin, end);
            if (er == Errors.REG_OKAY) {
                subset(t, begin, end);
            }
            return er;

        default:
            assert false;
        }
        return 0;
    }

    /**
     * - ccondissect - concatenation subexpression matches (with complications)
     * The retry memory stores the offset of the trial midpoint from begin,
     * plus 1 so that 0 uniquely means "clean slate".
     */
    int ccondissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;
        int er;

        assert t.op == '.';
        assert t.left != null && t.left.cnfa.nstates > 0;
        assert t.right != null && t.right.cnfa.nstates > 0;

        if (0 != (t.left.flags & Subre.SHORTER)) {		/* reverse scan */
            return crevdissect(t, begin, end);
        }

        d = new Dfa(this, t.left.cnfa);
        d2 = new Dfa(this, t.right.cnfa);

    /* pick a tentative midpoint */
        if (mem[t.retry] == 0) {
            mid = d.longest(begin, end, null);
            if (mid == -1) {
                return Errors.REG_NOMATCH;
            }
            mem[t.retry] = (mid - begin) + 1;
        } else {
            mid = begin + (mem[t.retry] - 1);
        }

    /* iterate until satisfaction or failure */
        for (; ; ) {
        /* try this midpoint on for size */
            er = cdissect(t.left, begin, mid);
            if (er == Errors.REG_OKAY &&
                    d2.longest(mid, end, null) == end &&
                    (er = cdissect(t.right, mid, end)) ==
                            Errors.REG_OKAY) {
                break;			/* NOTE BREAK OUT */
            }
            if (er != Errors.REG_OKAY && er != Errors.REG_NOMATCH) {
                return er;
            }

        /* that midpoint didn't work, find a new one */
            if (mid == begin) {
            /* all possibilities exhausted */
                return Errors.REG_NOMATCH;
            }
            mid = d.longest(begin, mid - 1, null);
            if (mid == -1) {
            /* failed to find a new one */
                return Errors.REG_NOMATCH;
            }
            mem[t.retry] = (mid - begin) + 1;
            zapmem(t.left);
            zapmem(t.right);
        }

    /* satisfaction */
        return Errors.REG_OKAY;
    }

    void zapmem(Subre t) {

    }    static final int UNTRIED = 0; 	/* not yet tried at all */

    /**
     * crevdissect - determine backref shortest-first subexpression matches
     * The retry memory stores the offset of the trial midpoint from begin,
     * plus 1 so that 0 uniquely means "clean slate".
     */
    int crevdissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;
        int er;

        assert t.op == '.';
        assert t.left != null && t.left.cnfa.nstates > 0;
        assert t.right != null && t.right.cnfa.nstates > 0;
        assert 0 != (t.left.flags & Subre.SHORTER);

    /* concatenation -- need to split the substring between parts */
        d = new Dfa(this, t.left.cnfa);
        d2 = new Dfa(this, t.right.cnfa);

    /* pick a tentative midpoint */
        if (mem[t.retry] == 0) {
            mid = d.shortest(begin, begin, end, null, null);
            if (mid == -1) {
                return Errors.REG_NOMATCH;
            }
            mem[t.retry] = (mid - begin) + 1;
        } else {
            mid = begin + (mem[t.retry] - 1);
        }

    /* iterate until satisfaction or failure */
        for (; ; ) {
        /* try this midpoint on for size */
            er = cdissect(t.left, begin, mid);
            if (er == Errors.REG_OKAY &&
                    d2.longest(mid, end, null) == end &&
                    (er = cdissect(t.right, mid, end)) ==
                            Errors.REG_OKAY) {
                break;			/* NOTE BREAK OUT */
            }
            if (er != Errors.REG_OKAY && er != Errors.REG_NOMATCH) {
                return er;
            }

        /* that midpoint didn't work, find a new one */
            if (mid == end) {
            /* all possibilities exhausted */
                return Errors.REG_NOMATCH;
            }
            mid = d.shortest(begin, mid + 1, end, null, null);
            if (mid == -1) {
            /* failed to find a new one */
                return Errors.REG_NOMATCH;
            }
            mem[t.retry] = (mid - begin) + 1;
            zapmem(t.left);
            zapmem(t.right);
        }

    /* satisfaction */
        return Errors.REG_OKAY;
    }    static final int TRYING = 1;    /* top matched, trying submatches */

    /**
     * cbrdissect - determine backref subexpression matches
     */
    int cbrdissect(Subre t, int begin, int end) {
        int i;
        int n = t.subno;
        int len;
        int paren;
        int p;
        int stop;
        int min = t.min;
        int max = t.max;

        assert t.op == 'b';
        assert n >= 0;


        if (match.get(n).start == -1) {
            return Errors.REG_NOMATCH;
        }
        paren = startIndex + match.get(n).start;
        len = match.get(n).end - match.get(n).start;

    /* no room to maneuver -- retries are pointless */
        if (0 != mem[t.retry]) {
            return Errors.REG_NOMATCH;
        }
        mem[t.retry] = 1;

    /* special-case zero-length string */
        if (len == 0) {
            if (begin == end) {
                return Errors.REG_OKAY;
            }
            return Errors.REG_NOMATCH;
        }

    /* and too-short string */
        assert (end >= begin);
        if ((end - begin) < len) {
            return Errors.REG_NOMATCH;
        }
        stop = end - len;

    /* count occurrences */
        i = 0;
        for (p = begin; p <= stop && (i < max || max == Compiler.INFINITY); p += len) {
            // paren is index of

            if (g.compare.compare(data, startIndex + paren, startIndex + p, len) != 0) {
                break;
            }
            i++;
        }

    /* and sort it out */
        if (p != end) {			/* didn't consume all of it */
            return Errors.REG_NOMATCH;
        }
        if (min <= i && (i <= max || max == Compiler.INFINITY)) {
            return Errors.REG_OKAY;
        }
        return Errors.REG_NOMATCH;		/* out of range */
    }    static final int TRIED = 2;     /* top didn't match or submatches exhausted */

    /*
     - caltdissect - determine alternative subexpression matches (w. complications)
     ^ static int caltdissect(struct vars *, struct Subre , int , int );
     */
    int caltdissect(Subre t, int begin, int end) {
        Dfa d;
        int er;
        if (t == null) {
            return Errors.REG_NOMATCH;
        }
        assert (t.op == '|');
        if (mem[t.retry] == TRIED) {
            return caltdissect(t.right, begin, end);
        }

        if (mem[t.retry] == UNTRIED) {
            d = new Dfa(this, t.left.cnfa);
            if (d.longest(begin, end, null) != end) {
                mem[t.retry] = TRIED;
                return caltdissect(t.right, begin, end);
            }
            mem[t.retry] = TRYING;
        }

        er = cdissect(t.left, begin, end);
        if (er != Errors.REG_NOMATCH) {
            return er;
        }

        mem[t.retry] = TRIED;
        return caltdissect(t.right, begin, end);
    }

    private int getMaxSubno(Subre tree, int i) {
        i = Math.max(i, tree.retry);
        if (tree.left != null) {
            i = Math.max(i, getMaxSubno(tree.left, i));
        }
        if (tree.right != null) {
            i = Math.max(i, getMaxSubno(tree.right, i));
        }
        return i;
    }
}
