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

import java.util.BitSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

/**
 * Runtime DFA.
 * Since we are not going to implement REG_SMALL,
 * the 'cache' in here is simply allowed to be a map
 * from state bitmaps to state sets. If we manage
 * to use enough memory to raise an eyebrow, we can
 * reconsider.
 */
class Dfa {
    static final Logger LOG = LoggerFactory.getLogger(Dfa.class);

    Object2ObjectLinkedOpenHashMap<BitSet, StateSet> stateSets;
    int nstates;
    int ncolors; // length of outarc and inchain vectors (really?)
    Cnfa cnfa;
    ColorMap cm;
    int lastpost; 	/* location of last cache-flushed success */
    int lastnopr; 	/* location of last cache-flushed NOPROGRESS */
    Runtime runtime;


    Dfa(Runtime runtime, Cnfa cnfa) {
        this.runtime = runtime;
        this.cm = runtime.g.cm;
        this.cnfa = cnfa;
        /*
         * To match the C behavior, we need to preserve insertion order.
         * Note that there's a pretty big assumption here that we can punt
         * the LRU-ish cache of C in favor of just letting them build up.
         * When Regexing through a large text this may not work so well.
         */
        stateSets = new Object2ObjectLinkedOpenHashMap<BitSet, StateSet>();
        nstates = cnfa.nstates;
        ncolors = cnfa.ncolors;
    }

    /**
     * Called at the start of a match.
     * arguably we could just construct a new DFA each time.
     */
    StateSet initialize(int start) {
        // Discard state sets; reuse would be faster if we kept them,
        // but then we'd need the real cache.
        stateSets.clear();
        StateSet stateSet = new StateSet(nstates, ncolors);
        stateSet.states.set(cnfa.pre);
        stateSet.flags = StateSet.STARTER
                | StateSet.LOCKED
                | StateSet.NOPROGRESS;
        // Insert into hash table based on that one state.
        stateSets.put(stateSet.states, stateSet);
        lastpost = -1;
        lastnopr = -1;
        stateSet.setLastSeen(start);
        return stateSet;
    }

    /**
     * 'miss' -- the state set was not found in the stateSets.
     *
     * @param co
     * @param cp
     * @return
     */
    StateSet miss(StateSet css, short co, int cp) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("miss: %s %d %d", css, co, cp));
        }
        if (css.outs[co] != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("hit!");
            }
            return css.outs[co];
        }

         /* first, what set of states would we end up in? */
        BitSet work = new BitSet(nstates);
        boolean ispost = false;
        boolean noprogress = true;
        boolean gotstate = false;

        for (int i = 0; i < nstates; i++) {
            if (css.states.get(i)) {
                long ca;
                int ax;
                short caco;
                int catarget;
                for (ax = cnfa.states[i] + 1,
                        ca = cnfa.arcs[ax],
                        caco = Cnfa.carcColor(ca),
                        catarget = Cnfa.carcTarget(ca);
                     caco != Constants.COLORLESS;
                     ax++, ca = cnfa.arcs[ax], caco = Cnfa.carcColor(ca), catarget = Cnfa.carcTarget(ca)) {

                    if (caco == co) {
                        work.set(catarget, true);
                        gotstate = true;
                        if (catarget == cnfa.post) {
                            ispost = true;
                        }
                        // get target state, index arcs, get color, compare to 0.
                        if (0 == Cnfa.carcColor(cnfa.arcs[cnfa.states[catarget]])) {
                            noprogress = false;
                        }
                        if (LOG.isDebugEnabled()) {
                            LOG.debug(String.format("%d -> %d", i, catarget));
                        }
                    }
                }
            }
        }
        boolean dolacons = gotstate && (0 != (cnfa.flags & Cnfa.HASLACONS));
        boolean sawlacons = false;
        while (dolacons) { /* transitive closure */
            dolacons = false;
            for (int i = 0; i < nstates; i++) {
                if (work.get(i)) {
                    long ca;
                    int ax;
                    short caco;
                    int catarget;
                    for (ax = cnfa.states[i] + 1, ca = cnfa.arcs[ax], caco = Cnfa.carcColor(ca), catarget = Cnfa.carcTarget(ca);
                            caco != Constants.COLORLESS;
                            ax++, ca = cnfa.arcs[ax], caco = Cnfa.carcColor(ca), catarget = Cnfa.carcTarget(ca)) {
                        if (caco <= ncolors) {
                            continue; /* NOTE CONTINUE */
                        }
                        sawlacons = true;
                        if (work.get(catarget)) {
                            continue; /* NOTE CONTINUE */
                        }
                        if (!lacon(cp, caco)) {
                            continue; /* NOTE CONTINUE */
                        }
                        work.set(catarget, true);
                        dolacons = true;
                        if (catarget == cnfa.post) {
                            ispost = true;
                        }
                        if (0 == Cnfa.carcColor(cnfa.arcs[cnfa.states[catarget]])) {
                            noprogress = false;
                        }

                        if (LOG.isDebugEnabled()) {
                            LOG.debug("%d :> %d", i, catarget);
                        }
                    }
                }
            }
        }

        if (!gotstate) {
            return null;
        }

        StateSet stateSet = stateSets.get(work);
        if (stateSet == null) {
            stateSet = new StateSet(nstates, ncolors);
            stateSet.ins = new Arcp(null, Constants.WHITE);
            stateSet.states = work;
            stateSet.flags = ispost ? StateSet.POSTSTATE : 0;
            if (noprogress) {
                stateSet.flags |= StateSet.NOPROGRESS;
            }
            /* lastseen to be dealt with by caller */
            stateSets.put(work, stateSet);
        }

        if (!sawlacons) {
            css.outs[co] = stateSet;
            css.inchain[co] = stateSet.ins;
            stateSet.ins = new Arcp(css, co);
        }

        return stateSet;
    }

    boolean lacon(int cp, short co) {
        int end;

        int n = co - cnfa.ncolors;
        // compare this to com.basistech.tclre.Nfa.compact(), the LACONS case.
        // that adds a.co to ncolors. So that means that you'd think that the lacons
        // indexing would be related... The 'arc' should have a 'color' which is an index
        // into lacon.
        assert n < runtime.g.lacons.size();
        Subre sub = runtime.g.lacons.get(n);
        Dfa d = new Dfa(runtime, sub.cnfa);
        end = d.longest(cp, runtime.endIndex, null);
        return (sub.subno != 0) ? (end != -1) : (end == -1);
    }

    /**
     * longest - longest-preferred matching engine
     *
     * @return endpoint or -1
     */
    int longest(int start, int stop, boolean[] hitstopp) {
        int cp;
        int realstop = (stop == runtime.endIndex) ? stop : stop + 1;
        short co;
        StateSet css;
        int post;

        if (LOG.isDebugEnabled()) {
            LOG.debug("+++ startup +++");
        }

    /* initialize */
        css = initialize(start);
        cp = start;
        if (hitstopp != null) {
            hitstopp[0] = false;
        }


    /* startup */
        if (cp == runtime.startIndex) {
            co = cnfa.bos[0 != (runtime.eflags & Flags.REG_NOTBOL) ? 0 : 1];
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("color %d", co));
            }
        } else {
            co = cm.getcolor(runtime.data[cp - 1]);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("char %c, color %d\n", runtime.data[cp - 1], co));
            }
        }
        css = miss(css, co, cp);
        if (css == null) {
            return -1;
        }
        css.setLastSeen(cp);

        StateSet ss;
    /* main loop */
        while (cp < realstop) {
            co = cm.getcolor(runtime.data[cp]);
            ss = css.outs[co];
            if (ss == null) {
                ss = miss(css, co, cp + 1);
                if (ss == null) {
                    break;	/* NOTE BREAK OUT */
                }
            }
            cp++;
            ss.setLastSeen(cp);
            css = ss;
        }

    /* shutdown */
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("+++ shutdown +++ at %s", css));
        }

        if (cp == runtime.endIndex && stop == runtime.endIndex) {
            if (hitstopp != null) {
                hitstopp[0] = true;
            }
            co = cnfa.eos[0 != (runtime.eflags & Flags.REG_NOTEOL) ? 0 : 1];
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("color %d", co));
            }
            ss = miss(css, co, cp);
        /* special case:  match ended at eol? */
            if (ss != null && (0 != (ss.flags & StateSet.POSTSTATE))) {
                return cp;
            } else if (ss != null) {
                ss.setLastSeen(cp);	/* to be tidy */
            }
        }

    /* find last match, if any */
        post = lastpost;
        ObjectIterator<Object2ObjectMap.Entry<BitSet, StateSet>> it = stateSets.object2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            StateSet thisSS = it.next().getValue();
            if (0 != (thisSS.flags & StateSet.POSTSTATE) && post != thisSS.getLastSeen()
                    && (post == -1 || post < thisSS.getLastSeen())) {
                post = thisSS.getLastSeen();
            }
        }
        if (post != -1) {		/* found one */
            return post - 1;
        }
        return -1;
    }


    /**
     * shortest - shortest-preferred matching engine
     *
     * @param start   where the match should start
     * @param min     match must end at or after here
     * @param max     match must end at or before here
     * @param coldp   store coldstart pointer here, if non-null
     * @param hitstop record whether hit end of total input/
     * @return endpoint or -1
     */
    int shortest(int start, int min, int max, int[] coldp, boolean[] hitstop) {
        int cp;
        int realmin = min == runtime.endIndex ? min : min + 1;
        int realmax = max == runtime.endIndex ? max : max + 1;
        short co;
        StateSet ss;
        StateSet css;

        if (LOG.isDebugEnabled()) {
            LOG.debug(" --- startup ---");
        }

    /* initialize */
        css = initialize(start);
        cp = start;
        if (hitstop != null) {
            hitstop[0] = false;
        }

    /* startup */
        if (cp == runtime.startIndex) {
            co = cnfa.bos[0 != (runtime.eflags & Flags.REG_NOTBOL) ? 0 : 1];
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("color %d", co));
            }
        } else {
            co = cm.getcolor(runtime.data[cp - 1]);
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("char %c, color %d\n", runtime.data[cp - 1], co));
            }
        }

        css = miss(css, co, cp);
        if (css == null) {
            return -1;
        }

        css.setLastSeen(cp);
        ss = css;

    /* main loop */
        while (cp < realmax) {
            co = cm.getcolor(runtime.data[cp]);
            ss = css.outs[co];
            if (ss == null) {
                ss = miss(css, co, cp + 1);
                if (ss == null) {
                    break;	/* NOTE BREAK OUT */
                }
            }
            cp++;
            ss.setLastSeen(cp);
            css = ss;
            if (0 != (ss.flags & StateSet.POSTSTATE) && cp >= realmin) {
                break;		/* NOTE BREAK OUT */
            }
        }


        if (ss == null) {
            return -1;
        }

        if (coldp != null) {	/* report last no-progress state set, if any */
            coldp[0] = lastcold();
        }

        if (0 != (ss.flags & StateSet.POSTSTATE) && cp > min) {
            assert cp >= realmin;
            cp--;
        } else if (cp == runtime.endIndex && max == runtime.endIndex) {
            co = cnfa.eos[0 != (runtime.eflags & Flags.REG_NOTEOL) ? 0 : 1];
            ss = miss(css, co, cp);
        /* match might have ended at eol */
            if ((ss == null || (0 == (ss.flags & StateSet.POSTSTATE)))
                    && hitstop != null) {
                hitstop[0] = true;
            }
        }

        if (ss == null || 0 == (ss.flags & StateSet.POSTSTATE)) {
            return -1;
        }

        return cp;
    }


    /**
     * lastcold - determine last point at which no progress had been made
     *
     * @return offset or -1
     */
    int lastcold() {

        int nopr = lastnopr;
        if (nopr == -1) {
            nopr = runtime.startIndex;
        }

        ObjectIterator<Object2ObjectMap.Entry<BitSet, StateSet>> it = stateSets.object2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Object2ObjectMap.Entry<BitSet, StateSet> entry = it.next();
            StateSet ss = entry.getValue();
            if (0 != (ss.flags & StateSet.NOPROGRESS) && nopr < ss.getLastSeen()) {
                nopr = ss.getLastSeen();
            }
        }
        return nopr;
    }
}

