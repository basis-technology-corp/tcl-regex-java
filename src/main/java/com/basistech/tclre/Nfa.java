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

/**
* Nfa representation.
*/
class Nfa {
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
        newarc(Compiler.EMPTY, (short) 0, from, to);
    }

    /**
     * Factory method for new states.
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
     * Convenience method to return a new state with flag = 0.
     * @return new state
     */
    State newState() {
        return newstate(0);
    }

    /**
     * get rid of a state, releasing all its arcs.
     * I'm not sure that all this is needed, as opposed to depending on the GC.
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
}
