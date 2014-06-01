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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Nfa representation.
 */
class Nfa {
    private static final Logger LOG = LoggerFactory.getLogger(Nfa.class);

    State pre;	/* pre-initial state */
    State init;	/* initial state */
    State finalState;	/* final state */
    State post;	/* post-final state */
    int nstates;		/* for numbering states */
    State states;	/* state-chain header */
    State slast;	/* tail of the chain */
    ColorMap cm;	/* the color map */
    short[] bos = new short[2];		/* colors, if any, assigned to BOS and BOL */
    short[] eos = new short[2];		/* colors, if any, assigned to EOS and EOL */
    //
    // may not be wanted ...
    Compiler v;		/* simplifies compile error reporting */
    Nfa parent;	/* parent NFA, if any */

    /**
     * New Nfa at the top level.
     *
     * @param cm
     */
    Nfa(ColorMap cm) {
        this.cm = cm;
    }

    Nfa(Nfa parent) {
        this.parent = parent;
        this.cm = parent.cm;
    }

    void newarc(int t, short co, State from, State to) {
        Arc a;

        assert from != null && to != null;

    /* check for duplicates */
        for (a = from.outs; a != null; a = a.outchain) {
            if (a.co == co && a.to == to && a.type == t) {
                return;
            }
        }

        a = new Arc();
        a.type = t;
        a.co = co;
        a.to = to;
        a.from = from;

    /*
     * Put the new arc on the beginning, not the end, of the chains.
     * Not only is this easier, it has the very useful side effect that
     * deleting the most-recently-added arc is the cheapest case rather
     * than the most expensive one.
     */
        a.inchain = to.ins;
        to.ins = a;
        a.outchain = from.outs;
        from.outs = a;

        from.nouts++;
        to.nins++;

        if (a.colored() && parent == null) {
            cm.colorchain(a);
        }
    }

    void emptyarc(State from, State to) {
        newarc(Compiler.EMPTY, (short)0, from, to);
    }

    /**
     * Factory method for new states.
     *
     * @return a new state wired into this Nfa.
     */
    State newstate(int flag) {
        State newState = new State();
        newState.no = nstates++; // a unique number.
        if (states == null) {
            states = newState;
        }
        if (slast != null) {
            assert slast.next == null;
            slast.next = newState;
        }
        newState.prev = slast;
        slast = newState;
        newState.flag = flag;
        return newState;
    }

    /**
     * moveouts - move all out arcs of a state to another state
     */
    void moveouts(State old, State newState) {
        Arc a;

        assert old != newState;

        while ((a = old.outs) != null) {
            cparc(a, newState, a.to);
            freearc(a);
        }
    }

    /**
     * moveins - move all in arcs of a state to another state
     * You might think this could be done better by just updating the
     * existing arcs, and you would be right if it weren't for the desire
     * for duplicate suppression, which makes it easier to just make new
     * ones to exploit the suppression built into newarc.
     */
    void moveins(State old, State newState) {
        Arc a;

        assert old != newState;

        while ((a = old.ins) != null) {
            cparc(a, a.from, newState);
            freearc(a);
        }
        assert old.nins == 0;
        assert old.ins == null;
    }

    /**
     * Convenience method to return a new state with flag = 0.
     *
     * @return new state
     */
    State newstate() {
        return newstate(0);
    }

    /**
     * get rid of a state, releasing all its arcs.
     * I'm not sure that all this is needed, as opposed to depending on the GC.
     *
     * @param s the state to dispose of.
     */
    void dropstate(State s) {
        Arc a;

        while ((a = s.ins) != null) {
            freearc(a);
        }

        while ((a = s.outs) != null) {
            freearc(a);
        }

        freestate(s);
    }

    /**
     * Unwire a state from the NFA.
     *
     * @param s the state
     */
    void freestate(State s) {
        assert s != null;
        assert s.nins == 0;
        assert s.nouts == 0;
        if (s.next != null) {
            s.next.prev = s.prev;
        } else {
            assert s == slast;
            slast = s.prev;
        }
        if (s.prev != null) {
            s.prev.next = s.next;
        } else {
            assert s == states;
            states = s.next;
        }
    }

    void freearc(Arc victim) {
        State from = victim.from;
        State to = victim.to;
        assert victim.type != 0;

        if (victim.colored() && parent != null) {
            cm.uncolorchain(victim);
        }

        assert from != null;
        assert from.outs != null;
        Arc a = from.outs;
        if (a == victim) { // first in chain
            from.outs = victim.outchain;
        } else {
            for (; a != null && a.outchain != victim; a = a.outchain) {
                continue;
            }
            assert a != null;
            a.outchain = victim.outchain;
        }
        from.nouts--;

           /* take it off target's in-chain */
        assert to != null;
        assert to.ins != null;
        a = to.ins;
        if (a == victim) {		/* simple case:  first in chain */
            to.ins = victim.inchain;
        } else {
            for (; a != null && a.inchain != victim; a = a.inchain) {
                continue;
            }
            assert a != null;
            a.inchain = victim.inchain;
        }
        to.nins--;
    }


    /**
     * cparc - allocate a new arc within an NFA, copying details from old one
     */
    void cparc(Arc oa, State from, State to) {
        newarc(oa.type, oa.co, from, to);
    }

    /**
     * dupnfa - duplicate sub-NFA
     * Another recursive traversal, this time using tmp to point to duplicates
     * as well as mark already-seen states.  (You knew there was a reason why
     * it's a state pointer, didn't you? :-))
     * @param start duplicate starting here
     * @param stop ending here.
     * @param from stringing dup from here
     * @param to here.
     */
    void dupnfa(State start, State stop, State from, State to) {
        if (start == stop) {
            newarc(Compiler.EMPTY, (short)0, from, to);
            return;
        }

        stop.tmp = to;
        duptraverse(start, from);
    /* done, except for clearing out the tmp pointers */

        stop.tmp = null;
        cleartraverse(start);
    }

    /**
     * duptraverse - recursive heart of dupnfa
     */
    void duptraverse(State s, State stmp) {
        Arc a;

        if (s.tmp != null) {
            return;		/* already done */
        }

        s.tmp = (stmp == null) ? newstate() : stmp;
        if (s.tmp == null) {
            assert v.err != 0;
            return;
        }

        for (a = s.outs; a != null && !v.iserr(); a = a.outchain) {
            duptraverse(a.to, null);
            assert a.to.tmp != null;
            cparc(a, s.tmp, a.to.tmp);
        }
    }

    /*
     - cleartraverse - recursive cleanup for algorithms that leave tmp ptrs set
     */
    void cleartraverse(State s) {
        Arc a;

        if (s.tmp == null) {
            return;
        }
        s.tmp = null;

        for (a = s.outs; a != null; a = a.outchain) {
            cleartraverse(a.to);
        }
    }


    /**
     * specialcolors - fill in special colors for an NFA
     */
    void specialcolors() {
    /* false colors for BOS, BOL, EOS, EOL */
        if (parent == null) {
            bos[0] = cm.pseudocolor();
            bos[1] = cm.pseudocolor();
            eos[0] = cm.pseudocolor();
            eos[1] = cm.pseudocolor();
        } else {
            assert (parent.bos[0] != Constants.COLORLESS);
            bos[0] = parent.bos[0];
            assert (parent.bos[1] != Constants.COLORLESS);
            bos[1] = parent.bos[1];
            assert (parent.eos[0] != Constants.COLORLESS);
            eos[0] = parent.eos[0];
            assert (parent.eos[1] != Constants.COLORLESS);
            eos[1] = parent.eos[1];
        }
    }

    /**
     * dumpnfa - dump an NFA in human-readable form
     */
    void dumpnfa() {
        State s;

        LOG.debug(String.format("pre %d, post %d", pre.no, post.no));
        if (bos[0] != Constants.COLORLESS) {
            LOG.debug(String.format(", bos [%d]", bos[0]));
        }
        if (bos[1] != Constants.COLORLESS) {
            LOG.debug(String.format(", bol [%d]", bos[1]));
        }
        if (eos[0] != Constants.COLORLESS) {
            LOG.debug(String.format(", eos [%d]", eos[0]));
        }
        if (eos[1] != Constants.COLORLESS) {
            LOG.debug(String.format(", eol [%d]", eos[1]));
        }
        for (s = states; s != null; s = s.next) {
            dumpstate(s);
        }
        if (parent == null) {
            cm.dumpcolors();
        }
    }

    /**
     * dumpstate - dump an NFA state in human-readable form
     */
    void dumpstate(State s) {
        Arc a;

        LOG.debug(String.format("%d%s%c", s.no, (s.tmp != null) ? "T" : "",
                (s.flag != 0) ? s.flag : '.'));
        if (s.prev != null && s.prev.next != s) {
            LOG.debug(String.format("\tstate chain bad\n"));
        }
        if (s.nouts == 0) {
            LOG.debug("\tno out arcs\n");
        } else {
            dumparcs(s);
        }
        for (a = s.ins; a != null; a = a.inchain) {
            if (a.to != s) {
                LOG.debug(String.format("\tlink from %d to %d on %d's in-chain\n",
                        a.from.no, a.to.no, s.no));
            }
        }
    }

    /**
     * dumparcs - dump out-arcs in human-readable form
     */
    void dumparcs(State s) {
        int pos;

        assert s.nouts > 0;
    /* printing arcs in reverse order is usually clearer */
        pos = dumprarcs(s.outs, s, 1);
        if (pos != 1) {
            LOG.debug("");
        }
    }

    /**
     * dumprarcs - dump remaining outarcs, recursively, in reverse order
     * @return resulting print position
     */
    int
    dumprarcs(Arc a, State s, int pos) {
        if (a.outchain != null) {
            pos = dumprarcs(a.outchain, s, pos);
        }
        dumparc(a, s);
        if (pos == 5) {
            LOG.debug("");
            pos = 1;
        } else {
            pos++;
        }
        return pos;
    }

    /**
     * dumparc - dump one outarc in readable form, including prefixing tab
     */
    void dumparc(Arc a, State s) {
        switch (a.type) {
        case Compiler.PLAIN:
            LOG.debug(String.format("[%d]", a.co));
            break;
        case Compiler.AHEAD:
            LOG.debug(String.format(">%d>", a.co));
            break;
        case Compiler.BEHIND:
            LOG.debug(String.format("<%d<", a.co));
            break;
        case Compiler.LACON:
            LOG.debug(String.format(":%d:", a.co));
            break;
        case '^':
        case '$':
            LOG.debug(String.format("%c%d", (char)a.type, a.co));
            break;
        case Compiler.EMPTY:
            break;
        default:
            LOG.debug(String.format("0x%x/0%lo", a.type, a.co));
            break;
        }
        if (a.from != s) {
            LOG.debug(String.format("?%d?", a.from.no));
        }
        LOG.debug("->");
        if (a.to == null) {
            LOG.debug("null");
            return;
        }
        LOG.debug(String.format("%d", a.to.no));

        Arc aa;
        for (aa = a.to.ins; aa != null; aa = aa.inchain) {
            if (aa == a)
                break;		/* NOTE BREAK OUT */
        }
        if (aa ==null) {
            LOG.debug("?!?");	/* missing from in-chain */
        }
    }
}
