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
    static final int INCOMPATIBLE = 1;
    static final int SATISFIED = 2;
    static final int COMPATIBLE = 3;
    private static final Logger LOG = LoggerFactory.getLogger(Nfa.class);
    State pre;  /* pre-initial state */
    State init; /* initial state */
    State finalState;   /* final state */
    State post; /* post-final state */
    int nstates;        /* for numbering states */
    State states;   /* state-chain header */
    State slast;    /* tail of the chain */
    ColorMap cm;    /* the color map */
    short[] bos = new short[2];     /* colors, if any, assigned to BOS and BOL */
    short[] eos = new short[2];     /* colors, if any, assigned to EOS and EOL */
    //
    // may not be wanted ...
    Compiler v;     /* simplifies compile error reporting */
    Nfa parent; /* parent NFA, if any */

    /**
     * New Nfa at the top level.
     *
     * @param cm
     */
    Nfa(ColorMap cm) {
        this.cm = cm;
        commoninit();
    }

    Nfa(Nfa parent) {
        this.parent = parent;
        this.cm = parent.cm;
        commoninit();
    }

    private void commoninit() {
        nstates = 0;
        bos[1] = Constants.COLORLESS;
        bos[0] = bos[1];
        eos[1] = Constants.COLORLESS;
        eos[0] = eos[1];
        post = newstate('@');   /* number 0 */
        pre = newstate('>');        /* number 1 */

        init = newstate();      /* may become invalid later */
        finalState = newstate();
        cm.rainbow(this, Compiler.PLAIN, Constants.COLORLESS, pre, init);
        newarc('^', (short)1, pre, init);
        newarc('^', (short)0, pre, init);
        cm.rainbow(this, Compiler.PLAIN, Constants.COLORLESS, finalState, post);
        newarc('$', (short)1, finalState, post);
        newarc('$', (short)0, finalState, post);
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
     * copyins - copy all in arcs of a state to another state
     */
    void copyins(State old, State newState) {
        Arc a;

        assert old != newState;

        for (a = old.ins; a != null; a = a.inchain) {
            cparc(a, a.from, newState);
        }
    }

    /**
     * copyouts - copy all out arcs of a state to another state
     */
    void copyouts(State old, State newState) {
        Arc a;

        assert old != newState;

        for (a = old.outs; a != null; a = a.outchain) {
            cparc(a, newState, a.to);
        }
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
        if (a == victim) {      /* simple case:  first in chain */
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
     *
     * @param start duplicate starting here
     * @param stop  ending here.
     * @param from  stringing dup from here
     * @param to    here.
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
            return;     /* already done */
        }

        s.tmp = (stmp == null) ? newstate() : stmp;
        if (s.tmp == null) {
            return;
        }

        for (a = s.outs; a != null; a = a.outchain) {
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
            assert parent.bos[0] != Constants.COLORLESS;
            bos[0] = parent.bos[0];
            assert parent.bos[1] != Constants.COLORLESS;
            bos[1] = parent.bos[1];
            assert parent.eos[0] != Constants.COLORLESS;
            eos[0] = parent.eos[0];
            assert parent.eos[1] != Constants.COLORLESS;
            eos[1] = parent.eos[1];
        }
    }

    /**
     * dumpnfa - dump an NFA in human-readable form
     */
    void dumpnfa() {
        State s;

        if (!LOG.isDebugEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("pre %d, post %d", pre.no, post.no));

        if (bos[0] != Constants.COLORLESS) {
            sb.append(String.format(", bos [%d]", bos[0]));
        }
        if (bos[1] != Constants.COLORLESS) {
            sb.append(String.format(", bol [%d]", bos[1]));
        }
        if (eos[0] != Constants.COLORLESS) {
            sb.append(String.format(", eos [%d]", eos[0]));
        }
        if (eos[1] != Constants.COLORLESS) {
            sb.append(String.format(", eol [%d]", eos[1]));
        }
        LOG.debug(sb.toString());

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

        if (!LOG.isDebugEnabled()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d%s%c", s.no, (s.tmp != null) ? "T" : "",
                (s.flag != 0) ? (char)s.flag : '.'));
        if (s.prev != null && s.prev.next != s) {
            sb.append(String.format("\tstate chain bad"));
        }
        if (s.nouts == 0) {
            sb.append("\tno out arcs");
        } else {
            dumparcs(s, sb);
        }

        LOG.debug(sb.toString());
        for (a = s.ins; a != null; a = a.inchain) {
            if (a.to != s) {
                LOG.debug(String.format("\tlink from %d to %d on %d's in-chain",
                        a.from.no, a.to.no, s.no));
            }
        }
    }

    /**
     * dumparcs - dump out-arcs in human-readable form
     */
    void dumparcs(State s, StringBuilder sb) {
        int pos;

        assert s.nouts > 0;
    /* printing arcs in reverse order is usually clearer */
        pos = dumprarcs(s.outs, s, 1, sb);
        if (pos != 1) {
            //sb.append("\n");
        }
    }

    /**
     * dumprarcs - dump remaining outarcs, recursively, in reverse order
     *
     * @return resulting print position
     */
    int
    dumprarcs(Arc a, State s, int pos, StringBuilder sb) {
        if (a.outchain != null) {
            pos = dumprarcs(a.outchain, s, pos, sb);
        }
        dumparc(a, s, sb);
        if (pos == 5) {
            sb.append("\n");
            pos = 1;
        } else {
            pos++;
        }
        return pos;
    }

    /**
     * dumparc - dump one outarc in readable form, including prefixing tab
     */
    void dumparc(Arc a, State s, StringBuilder sb) {

        sb.append("\t");
        switch (a.type) {
        case Compiler.PLAIN:
            sb.append(String.format("[%d]", a.co));
            break;
        case Compiler.AHEAD:
            sb.append(String.format(">%d>", a.co));
            break;
        case Compiler.BEHIND:
            sb.append(String.format("<%d<", a.co));
            break;
        case Compiler.LACON:
            sb.append(String.format(":%d:", a.co));
            break;
        case '^':
        case '$':
            sb.append(String.format("%c%d", (char)a.type, a.co));
            break;
        case Compiler.EMPTY:
            break;
        default:
            sb.append(String.format("0x%x/0%lo", a.type, a.co));
            break;
        }
        if (a.from != s) {
            sb.append(String.format("?%d?", a.from.no));
        }
        sb.append("->");
        if (a.to == null) {
            sb.append("null");
            Arc aa;
            for (aa = a.to.ins; aa != null; aa = aa.inchain) {
                if (aa == a) {
                    break;      /* NOTE BREAK OUT */
                }
            }
            if (aa == null) {
                LOG.debug("?!?");   /* missing from in-chain */
            }
        } else {
            sb.append(String.format("%d", a.to.no));
        }
    }

    /**
     * optimize - optimize an NFA
     *
     * @return re_info bits
     */
    long optimize() throws RegexException {
        LOG.debug("initial cleanup");
        cleanup();      /* may simplify situation */
        dumpnfa();
        LOG.debug("empties");
        fixempties();   /* get rid of EMPTY arcs */
        LOG.debug("constraints");
        pullback(); /* pull back constraints backward */
        pushfwd();  /* push fwd constraints forward */
        LOG.debug("final cleanup");
        cleanup();      /* final tidying */
        return analyze();   /* and analysis */
    }

    /**
     * analyze - ascertain potentially-useful facts about an optimized NFA
     *
     * @return re_info bits.
     */
    long analyze() {
        Arc a;
        Arc aa;

        if (pre.outs == null) {
            return Flags.REG_UIMPOSSIBLE;
        }
        for (a = pre.outs; a != null; a = a.outchain) {
            for (aa = a.to.outs; aa != null; aa = aa.outchain) {
                if (aa.to == post) {
                    return Flags.REG_UEMPTYMATCH;
                }
            }
        }
        return 0;
    }

    /**
     * pullback - pull back constraints backward to (with luck) eliminate them
     */
    void pullback() throws RegexException {
        State s;
        State nexts;
        Arc a;
        Arc nexta;
        boolean progress;

    /* find and pull until there are no more */
        do {
            progress = false;
            for (s = states; s != null; s = nexts) {
                nexts = s.next;
                for (a = s.outs; a != null; a = nexta) {
                    nexta = a.outchain;
                    if (a.type == '^' || a.type == Compiler.BEHIND) {
                        if (pull(a)) {
                            progress = true;
                        }
                    }
                    assert nexta == null || s.no != State.FREESTATE;
                }
            }
            if (progress) {
                dumpnfa();
            }
        } while (progress);

        for (a = pre.outs; a != null; a = nexta) {
            nexta = a.outchain;
            if (a.type == '^') {
                assert a.co == 0 || a.co == 1;
                newarc(Compiler.PLAIN, bos[a.co], a.from, a.to);
                freearc(a);
            }
        }
    }

    /**
     * - pull - pull a back constraint backward past its source state
     * A significant property of this function is that it deletes at most
     * one state -- the constraint's from state -- and only if the constraint
     * was that state's last outarc.
     *
     * @return success
     */
    boolean pull(Arc con) throws RegexException {
        State from = con.from;
        State to = con.to;
        Arc a;
        Arc nexta;
        State s;

        if (from == to) {   /* circular constraint is pointless */
            freearc(con);
            return true;
        }
        if (0 != from.flag) {   /* can't pull back beyond start */
            return false;
        }
        if (from.nins == 0) {   /* unreachable */
            freearc(con);
            return true;
        }

    /* first, clone from state if necessary to avoid other outarcs */
        if (from.nouts > 1) {
            s = newstate();

            assert (to != from);        /* con is not an inarc */
            copyins(from, s);       /* duplicate inarcs */
            cparc(con, s, to);      /* move constraint arc */
            freearc(con);
            from = s;
            con = from.outs;
        }
        assert from.nouts == 1;

    /* propagate the constraint into the from state's inarcs */
        for (a = from.ins; a != null; a = nexta) {
            nexta = a.inchain;
            switch (combine(con, a)) {
            case INCOMPATIBLE:  /* destroy the arc */
                freearc(a);
                break;
            case SATISFIED:     /* no action needed */
                break;
            case COMPATIBLE:    /* swap the two arcs, more or less */
                s = newstate();
                cparc(a, s, to);        /* anticipate move */
                cparc(con, a.from, s);
                freearc(a);
                break;
            default:
                throw new RegexException("REG_ASSERT");
            }
        }

    /* remaining inarcs, if any, incorporate the constraint */
        moveins(from, to);
        dropstate(from);        /* will free the constraint */
        return true;
    }

    /**
     * pushfwd - push forward constraints forward to (with luck) eliminate them
     */
    void pushfwd() throws RegexException {
        State s;
        State nexts;
        Arc a;
        Arc nexta;
        boolean progress;

    /* find and push until there are no more */
        do {
            progress = false;
            for (s = states; s != null; s = nexts) {
                nexts = s.next;
                for (a = s.ins; a != null; a = nexta) {
                    nexta = a.inchain;
                    if (a.type == '$' || a.type == Compiler.AHEAD) {
                        if (push(a)) {
                            progress = true;
                        }
                    }
                    assert nexta == null || s.no != State.FREESTATE;
                }
            }
            if (progress) {
                dumpnfa();
            }
        } while (progress);

        for (a = post.ins; a != null; a = nexta) {
            nexta = a.inchain;
            if (a.type == '$') {
                assert a.co == 0 || a.co == 1;
                newarc(Compiler.PLAIN, eos[a.co], a.from, a.to);
                freearc(a);
            }
        }
    }

    /**
     * push - push a forward constraint forward past its destination state
     * A significant property of this function is that it deletes at most
     * one state -- the constraint's to state -- and only if the constraint
     * was that state's last inarc.
     */
    boolean push(Arc con) throws RegexException {
        State from = con.from;
        State to = con.to;
        Arc a;
        Arc nexta;
        State s;

        if (to == from) {   /* circular constraint is pointless */
            freearc(con);
            return true;
        }
        if (0 != to.flag) {     /* can't push forward beyond end */
            return false;
        }
        if (to.nouts == 0) {    /* dead end */
            freearc(con);
            return true;
        }

    /* first, clone to state if necessary to avoid other inarcs */
        if (to.nins > 1) {
            s = newstate();

            copyouts(to, s);        /* duplicate outarcs */
            cparc(con, from, s);    /* move constraint */
            freearc(con);
            to = s;
            con = to.ins;
        }
        assert to.nins == 1;

    /* propagate the constraint into the to state's outarcs */
        for (a = to.outs; a != null; a = nexta) {
            nexta = a.outchain;
            switch (combine(con, a)) {
            case INCOMPATIBLE:  /* destroy the arc */
                freearc(a);
                break;
            case SATISFIED:     /* no action needed */
                break;
            case COMPATIBLE:    /* swap the two arcs, more or less */
                s = newstate();
                cparc(con, s, a.to);    /* anticipate move */
                cparc(a, from, s);
                freearc(a);
                break;
            default:
                throw new RegexException("REG_ASSERT");
            }
        }

    /* remaining outarcs, if any, incorporate the constraint */
        moveouts(to, from);
        dropstate(to);      /* will free the constraint */
        return true;
    }

    /**
     * combine - constraint lands on an arc, what happens?
     *
     * @return result
     */
    int combine(Arc con, Arc a) throws RegexException {
        //# define  CA(ct,at)   (((ct)<<CHAR_BIT) | (at))

        //CA(con->type, a->type)) {
        switch ((con.type << 8) | a.type) {

        case '^' << 8 | Compiler.PLAIN:     /* newlines are handled separately */
        case '$' << 8 | Compiler.PLAIN:
            return INCOMPATIBLE;

        case Compiler.AHEAD << 8 | Compiler.PLAIN:      /* color constraints meet colors */
        case Compiler.BEHIND << 8 | Compiler.PLAIN:
            if (con.co == a.co) {
                return SATISFIED;
            }
            return INCOMPATIBLE;

        case '^' << 8 | '^':        /* collision, similar constraints */
        case '$' << 8 | '$':
        case Compiler.AHEAD << 8 | Compiler.AHEAD:
        case Compiler.BEHIND << 8 | Compiler.BEHIND:
            if (con.co == a.co) {       /* true duplication */
                return SATISFIED;
            }
            return INCOMPATIBLE;

        case '^' << 8 | Compiler.BEHIND:        /* collision, dissimilar constraints */
        case Compiler.BEHIND << 8 | '^':
        case '$' << 8 | Compiler.AHEAD:
        case Compiler.AHEAD << 8 | '$':
            return INCOMPATIBLE;

        case '^' << 8 | Compiler.AHEAD:
        case Compiler.BEHIND << 8 | '$':
        case Compiler.BEHIND << 8 | Compiler.AHEAD:
        case '$' << 8 | '^':
        case '$' << 8 | Compiler.BEHIND:
        case Compiler.AHEAD << 8 | '^':
        case Compiler.AHEAD << 8 | Compiler.BEHIND:
        case '^' << 8 | Compiler.LACON:
        case Compiler.BEHIND << 8 | Compiler.LACON:
        case '$' << 8 | Compiler.LACON:
        case Compiler.AHEAD << 8 | Compiler.LACON:
            return COMPATIBLE;

        }
        throw new RegexException("REG_ASSERT");
    }


    /**
     * cleanup - clean up NFA after optimizations
     */
    void cleanup() {
        State s;
        State nexts;
        int n;

    /* clear out unreachable or dead-end states */
    /* use pre to mark reachable, then post to mark can-reach-post */
        markreachable(pre, null, pre);
        markcanreach(post, pre, post);
        for (s = states; s != null; s = nexts) {
            nexts = s.next;
            if (s.tmp != post && 0 == s.flag) {
                dropstate(s);
            }
        }
        assert post.nins == 0 || post.tmp == post;

        cleartraverse(pre);

        assert post.nins == 0 || post.tmp == null;
    /* the nins==0 (final unreachable) case will be caught later */

    /* renumber surviving states */
        n = 0;
        for (s = states; s != null; s = s.next) {
            s.no = n++;
        }
        nstates = n;
    }

    /**
     * markreachable - recursive marking of reachable states
     *
     * @param s    a state
     * @param okay consider only states with this mark.
     * @param mark the value to mark with
     */
    void markreachable(State s, State okay, State mark) {
        Arc a;

        if (s.tmp != okay) {
            return;
        }

        s.tmp = mark;

        for (a = s.outs; a != null; a = a.outchain) {
            markreachable(a.to, okay, mark);
        }
    }

    /**
     * markcanreach - recursive marking of states which can reach here
     */
    void markcanreach(State s, State okay, State mark) {
        Arc a;

        if (s.tmp != okay) {
            return;
        }
        s.tmp = mark;

        for (a = s.ins; a != null; a = a.inchain) {
            markcanreach(a.from, okay, mark);
        }
    }

    /**
     * fixempties - get rid of EMPTY arcs
     */
    void fixempties() {
        State s;
        State nexts;
        Arc a;
        Arc nexta;
        boolean progress;

    /* find and eliminate empties until there are no more */
        do {
            progress = false;
            for (s = states; s != null; s = nexts) {
                nexts = s.next;
                for (a = s.outs; a != null; a = nexta) {
                    nexta = a.outchain;
                    if (a.type == Compiler.EMPTY && unempty(a)) {
                        progress = true;
                    }
                    assert nexta == null || s.no != State.FREESTATE;
                }
            }
            if (progress) {
                dumpnfa();
            }
        } while (progress);
    }

    /**
     * unempty - optimize out an EMPTY arc, if possible
     * Actually, as it stands this function always succeeds, but the return
     * value is kept with an eye on possible future changes.
     */
    boolean unempty(Arc a) {
        State from = a.from;
        State to = a.to;
        boolean usefrom;        /* work on from, as opposed to to? */

        assert a.type == Compiler.EMPTY;
        assert from != pre && to != post;

        if (from == to) {       /* vacuous loop */
            freearc(a);
            return true;
        }

    /* decide which end to work on */
        usefrom = true;         /* default:  attack from */
        if (from.nouts > to.nins) {
            usefrom = false;
        } else if (from.nouts == to.nins) {
        /* decide on secondary issue:  move/copy fewest arcs */
            if (from.nins > to.nouts) {
                usefrom = false;
            }
        }

        freearc(a);
        if (usefrom) {
            if (from.nouts == 0) {
            /* was the state's only outarc */
                moveins(from, to);
                freestate(from);
            } else {
                copyins(from, to);
            }
        } else {
            if (to.nins == 0) {
            /* was the state's only inarc */
                moveouts(to, from);
                freestate(to);
            } else {
                copyouts(to, from);
            }
        }

        return true;
    }

    Cnfa compact() {
        return null;
    }
}
