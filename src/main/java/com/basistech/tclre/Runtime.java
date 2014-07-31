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

import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Lists;

/**
 * The internal implementation of matching.
 */
class Runtime {
    static final int UNTRIED = 0;   /* not yet tried at all */
    static final int TRYING = 1;    /* top matched, trying submatches */
    static final int TRIED = 2;     /* top didn't match or submatches exhausted */

    HsrePattern re;
    Guts g;
    int eflags;
    List<RegMatch> match;
    RegMatch details;
    CharSequence data;
    int dataLength; // cache this, it gets examined _a lot_.
    int[] mem; // backtracking.

    /**
     * exec - match regular expression
     */
    boolean exec(HsrePattern re, CharSequence data, EnumSet<ExecFlags> execFlags) throws RegexException {
    /* sanity checks */
    /* setup */

        if (0 != (re.guts.info & Flags.REG_UIMPOSSIBLE)) {
            throw new RegexException("Regex marked impossible");
        }

        eflags = 0;
        for (ExecFlags ef : execFlags) {
            switch (ef) {
            case NOTBOL:
                eflags |= Flags.REG_NOTBOL;
                break;
            case NOTEOL:
                eflags |= Flags.REG_NOTEOL;
                break;
            default:
                throw new RuntimeException("impossible exec flag");
            }
        }

        this.re = re;
        this.g = re.guts;
        this.data = data;
        this.dataLength = this.data.length();
        this.match = Lists.newArrayList();
        match.add(null); // make room for 1.
        if (0 != (g.info & Flags.REG_UBACKREF)) {
            while (match.size() < g.nsub + 1) {
                match.add(null);
            }
        }
        mem = new int[g.ntree];
       
    /* do it */
        assert g.tree != null;

        if (0 != (g.info & Flags.REG_UBACKREF)) {
            return cfind(g.tree.cnfa);
        } else {
            return find(g.tree.cnfa);
        }
    }

    /**
     * find - find a match for the main NFA (no-complications case)
     */
    boolean find(Cnfa cnfa) {
        int begin;
        int end = -1;
        int cold;
        int open;       /* open and close of range of possible starts */
        int close;
        boolean hitend;
        boolean shorter = 0 != (g.tree.flags & Subre.SHORTER);

    /* first, a shot with the search RE */
        Dfa s = new Dfa(this, g.search);
        int[] coldp = new int[1];
        close = s.shortest(0, 0, data.length(), coldp, null);
        cold = coldp[0];

        int dtstart;
        if (cold != -1) {
            dtstart = cold;
        } else {
            dtstart = data.length();
        }
        details = new RegMatch(dtstart, data.length());

        if (close == -1) {      /* not found */
            return false;
        }

    /* find starting point and match */
        assert cold != -1;
        open = cold;

        cold = -1;
        Dfa d = new Dfa(this, cnfa);
        for (begin = open; begin <= close; begin++) {
            boolean[] hitendp = new boolean[1];
            if (shorter) {
                end = d.shortest(begin, begin, data.length(), null, hitendp);
            } else {
                end = d.longest(begin, data.length(), hitendp);
            }
            hitend = hitendp[0];

            if (hitend && cold == -1) {
                cold = begin;
            }
            if (end != -1) {
                break;      /* NOTE BREAK OUT */
            }
        }

        assert end != -1;       /* search RE succeeded so loop should */

        /* and pin down details */
        match.set(0, new RegMatch(begin, end));

        if (cold != -1) {
            dtstart = cold;
        } else {
            dtstart = data.length();
        }
        details = new RegMatch(dtstart, data.length());

        if (re.nsub > 0) { // no need to do the work.
            return dissect(g.tree, begin, end);
        } else {
            return true;
        }
    }


    /**
     * cfind - find a match for the main NFA (with complications)
     */
    boolean cfind(Cnfa cnfa) {
        int[] cold = new int[1];

        Dfa s = new Dfa(this, g.search);
        Dfa d = new Dfa(this, cnfa);

        boolean ret = cfindloop(cnfa, d, s, cold);

        int dtstart;
        if (cold[0] != -1) {
            dtstart = cold[0];
        } else {
            dtstart = data.length();
        }

        details = new RegMatch(dtstart, data.length());
        return ret;
    }

    /**
     * cfindloop - the heart of cfind
     */
    boolean cfindloop(Cnfa cnfa, Dfa d, Dfa s, int[] coldp) {
        int begin;
        int end;
        int cold;
        int open;       /* open and close of range of possible starts */
        int close;
        int estart;
        int estop;
        boolean shorter = 0 != (g.tree.flags & Subre.SHORTER);
        boolean hitend[] = new boolean[1];

        assert d != null && s != null;
        close = 0;
        do {
            int[] cold0 = new int[1];
            close = s.shortest(close, close, data.length(), cold0, null);
            cold = cold0[0];

            if (close == -1) {
                break;              /* NOTE BREAK */
            }
            assert cold != -1;
            open = cold;
            cold = -1;

            for (begin = open; begin <= close; begin++) {
                estart = begin;
                estop = data.length();
                for (;;) {
                    if (shorter) {
                        end = d.shortest(begin, estart, estop, null, hitend);
                    } else {
                        end = d.longest(begin, estop, hitend);
                    }
                    if (hitend[0] && cold == -1) {
                        cold = begin;
                    }
                    if (end == -1) {
                        break;      /* NOTE BREAK OUT */
                    }

                    for (int x = 0; x < match.size(); x++) {
                        match.set(x, null);
                    }

                    int maxsubno = getMaxSubno(g.tree, 0);
                    mem = new int[maxsubno + 1];
                    boolean matched = cdissect(g.tree, begin, end);
                    if (matched) {
                        // indicate the full match bounds.
                        match.set(0, new RegMatch(begin, end));
                        coldp[0] = cold;
                        return true;
                    }
                    if (shorter ? end == estop : end == begin) {
                        /* no point in trying again */
                        coldp[0] = cold;
                        return false;
                    }
                    /* go around and try again */
                    if (shorter) {
                        estart = end + 1;
                    } else {
                        estop = end - 1;
                    }
                }
            }
        } while (close < data.length());

        coldp[0] = cold;
        return false;
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
    boolean dissect(Subre t, int begin, int end) {
        switch (t.op) {
        case '=':       /* terminal node */
            assert t.left == null && t.right == null;
            return true;    /* no action, parent did the work */

        case '|':       /* alternation */
            assert t.left != null;
            return altdissect(t, begin, end);

        case 'b':       /* back ref -- shouldn't be calling us! */
            throw new RuntimeException("impossible backref");

        case '.':       /* concatenation */
            assert t.left != null && t.right != null;
            return condissect(t, begin, end);

        case '(':       /* capturing */
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
    boolean condissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;
        boolean shorter = (t.left.flags & Subre.SHORTER) != 0;
        int stop = shorter ? end : begin;

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
        boolean dissectMatch = dissect(t.left, begin, mid);
        if (!dissectMatch) {
            return false;
        }
        return dissect(t.right, mid, end);
    }

    /**
     * altdissect - determine alternative subexpression matches (uncomplicated)
     */
    boolean altdissect(Subre t, int begin, int end) {
        Dfa d;

        assert t != null;
        assert t.op == '|';

        for (; t != null; t = t.right) {
            assert t.left != null && t.left.cnfa.nstates > 0;
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
    boolean cdissect(Subre t, int begin, int end) {

        assert t != null;

        switch (t.op) {
        case '=':       /* terminal node */
            assert t.left == null && t.right == null;
            return true;    /* no action, parent did the work */

        case '|':       /* alternation */
            assert t.left != null;
            return caltdissect(t, begin, end);

        case 'b':       /* back ref -- shouldn't be calling us! */
            assert t.left == null && t.right == null;
            return cbrdissect(t, begin, end);

        case '.':       /* concatenation */
            assert t.left != null && t.right != null;
            return ccondissect(t, begin, end);

        case '(':       /* capturing */
            assert t.left != null && t.right == null;
            assert t.subno > 0;
            boolean cdmatch = cdissect(t.left, begin, end);
            if (cdmatch) {
                subset(t, begin, end);
            }
            return cdmatch;

        default:
            throw new RuntimeException("Impossible op");
        }
    }

    /**
     * - ccondissect - concatenation subexpression matches (with complications)
     * The retry memory stores the offset of the trial midpoint from begin,
     * plus 1 so that 0 uniquely means "clean slate".
     */
    boolean ccondissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;

        assert t.op == '.';
        assert t.left != null && t.left.cnfa.nstates > 0;
        assert t.right != null && t.right.cnfa.nstates > 0;

        if (0 != (t.left.flags & Subre.SHORTER)) {      /* reverse scan */
            return crevdissect(t, begin, end);
        }

        d = new Dfa(this, t.left.cnfa);
        d2 = new Dfa(this, t.right.cnfa);

    /* pick a tentative midpoint */
        if (mem[t.retry] == 0) {
            mid = d.longest(begin, end, null);
            if (mid == -1) {
                return false;
            }
            mem[t.retry] = (mid - begin) + 1;
        } else {
            mid = begin + (mem[t.retry] - 1);
        }

    /* iterate until satisfaction or failure */
        for (;;) {
        /* try this midpoint on for size */
            boolean cdmatch = cdissect(t.left, begin, mid);
            if (cdmatch && d2.longest(mid, end, null) == end
                    && (cdissect(t.right, mid, end))) {
                break;          /* NOTE BREAK OUT */

            }

        /* that midpoint didn't work, find a new one */
            if (mid == begin) {
            /* all possibilities exhausted */
                return false;
            }
            mid = d.longest(begin, mid - 1, null);
            if (mid == -1) {
            /* failed to find a new one */
                return false;
            }
            mem[t.retry] = (mid - begin) + 1;
            zapmem(t.left);
            zapmem(t.right);
        }

    /* satisfaction */
        return true;
    }

    void zapmem(Subre t) {
        mem[t.retry] = 0;
        while (match.size() < t.subno + 1) {
            match.add(null);
        }
        if (t.left != null) {
            zapmem(t.left);
        }
        if (t.right != null) {
            zapmem(t.right);
        }
    }


    /**
     * crevdissect - determine backref shortest-first subexpression matches
     * The retry memory stores the offset of the trial midpoint from begin,
     * plus 1 so that 0 uniquely means "clean slate".
     */
    boolean crevdissect(Subre t, int begin, int end) {
        Dfa d;
        Dfa d2;
        int mid;

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
                return false;
            }
            mem[t.retry] = (mid - begin) + 1;
        } else {
            mid = begin + (mem[t.retry] - 1);
        }

    /* iterate until satisfaction or failure */
        for (;;) {
        /* try this midpoint on for size */
            boolean cdmatch = cdissect(t.left, begin, mid);
            if (cdmatch
                    && d2.longest(mid, end, null) == end
                    && (cdissect(t.right, mid, end))) {
                break;          /* NOTE BREAK OUT */
            }

        /* that midpoint didn't work, find a new one */
            if (mid == end) {
            /* all possibilities exhausted */
                return false;
            }
            mid = d.shortest(begin, mid + 1, end, null, null);
            if (mid == -1) {
            /* failed to find a new one */
                return false;
            }
            mem[t.retry] = (mid - begin) + 1;
            zapmem(t.left);
            zapmem(t.right);
        }

    /* satisfaction */
        return true;
    }


    /**
     * cbrdissect - determine backref subexpression matches
     */
    boolean cbrdissect(Subre t, int begin, int end) {
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

        //TODO: could this get be out of range?
        if (match.get(n) == null) {
            return false;
        }
        paren = match.get(n).start;
        len = match.get(n).end - match.get(n).start;

    /* no room to maneuver -- retries are pointless */
        if (0 != mem[t.retry]) {
            return false;
        }
        mem[t.retry] = 1;

    /* special-case zero-length string */
        if (len == 0) {
            if (begin == end) {
                return true;
            }
            return false;
        }

        /* and too-short string */
        assert end >= begin;
        if ((end - begin) < len) {
            return false;
        }
        stop = end - len;

    /* count occurrences */
        i = 0;
        for (p = begin; p <= stop && (i < max || max == Compiler.INFINITY); p += len) {
            // paren is index of

            if (g.compare.compare(data, paren, p, len) != 0) {
                break;
            }
            i++;
        }

    /* and sort it out */
        if (p != end) {         /* didn't consume all of it */
            return false;
        }
        if (min <= i && (i <= max || max == Compiler.INFINITY)) {
            return true;
        }
        return false;       /* out of range */
    }

    /*
     - caltdissect - determine alternative subexpression matches (w. complications)
     ^ static int caltdissect(struct vars *, struct Subre , int , int );
     */
    boolean caltdissect(Subre t, int begin, int end) {
        Dfa d;
        if (t == null) {
            return false;
        }
        assert t.op == '|';
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

        boolean cdmatch = cdissect(t.left, begin, end);
        if (cdmatch) {
            return true;
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
