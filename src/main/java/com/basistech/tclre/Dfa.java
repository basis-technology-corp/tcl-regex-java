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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.BitSet;

/**
 * Runtime DFA.
 */
class Dfa {
    final Object2ObjectMap<BitSet, StateSet> stateSets;
    final int nstates;
    final int ncolors; // length of outarc and inchain vectors (really?)
    final Cnfa cnfa;
    final RuntimeColorMap cm;
    final Runtime runtime;

    Dfa(Runtime runtime, Cnfa cnfa) {
        this.runtime = runtime;
        this.cm = runtime.g.cm;
        this.cnfa = cnfa;
        /*
         * Note that this isn't a cache;
         * Benson believes that the maximum size here is proportional
         * to the complexity of the machine, not to the input.
         */
        stateSets = new Object2ObjectOpenHashMap<BitSet, StateSet>();
        nstates = cnfa.states.length;
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
        stateSet.noprogress = true;
        // Insert into hash table based on that one state.
        stateSets.put(stateSet.states, stateSet);
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
        if (css.outs[co] != null) {
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
                        work.set(catarget);
                        gotstate = true;
                        if (catarget == cnfa.post) {
                            ispost = true;
                        }
                        // get target state, index arcs, get color, compare to 0.
                        if (0 == Cnfa.carcColor(cnfa.arcs[cnfa.states[catarget]])) {
                            noprogress = false;
                        }
                    }
                }
            }
        }
        boolean dolacons = gotstate && cnfa.hasLacons;
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
                        work.set(catarget);
                        dolacons = true;
                        if (catarget == cnfa.post) {
                            ispost = true;
                        }
                        if (0 == Cnfa.carcColor(cnfa.arcs[cnfa.states[catarget]])) {
                            noprogress = false;
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
            stateSet = new StateSet(work, ncolors);
            stateSet.ins = new Arcp(null, Constants.WHITE);
            stateSet.poststate = ispost;
            stateSet.noprogress |= noprogress;
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
        //
        RuntimeSubexpression subex = runtime.g.lookaheadConstraintMachine(n);
        Dfa d = new Dfa(runtime, subex.machine);
        end = d.longest(cp, runtime.data.length(), null);
        return (subex.number != 0) ? (end != -1) : (end == -1);
    }

    /**
     * longest - longest-preferred matching engine
     *
     * @return endpoint or -1
     */
    int longest(int start, int stop, boolean[] hitstop) {
        int cp;
        int realstop = (stop == runtime.dataLength) ? stop : stop + 1;
        short co;
        StateSet css;
        int post;

    /* initialize */
        css = initialize(start);
        cp = start;
        if (hitstop != null) {
            hitstop[0] = false;
        }

    /* startup */
        if (cp == 0) {
            co = cnfa.bos[0 != (runtime.eflags & Flags.REG_NOTBOL) ? 0 : 1];
        } else {
            char theChar = runtime.data.charAt(cp - 1);
            if (Character.isLowSurrogate(theChar)) {
                // collect the other end of the surrogate.
                char high = theChar = runtime.data.charAt(cp - 2);
                int codepoint = Character.toCodePoint(high, theChar);
                co = cm.getcolor(codepoint); // and get a color for the pair.
            } else {
                co = cm.getcolor(theChar);
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
            char theChar = runtime.data.charAt(cp);
            int increment = 1;
            if (Character.isHighSurrogate(theChar)) {
                int codepoint = Character.toCodePoint(theChar, runtime.data.charAt(cp + 1));
                co = cm.getcolor(codepoint);
                increment = 2;
            } else {
                co = cm.getcolor(theChar);
            }
            ss = css.outs[co];
            if (ss == null) {
                ss = miss(css, co, cp + increment);
                if (ss == null) {
                    break;  /* NOTE BREAK OUT */
                }
            }

            cp = cp + increment;
            ss.setLastSeen(cp);
            css = ss;
        }

    /* shutdown */
        if (cp == runtime.dataLength && stop == runtime.dataLength) {
            if (hitstop != null) {
                hitstop[0] = true;
            }
            co = cnfa.eos[0 != (runtime.eflags & Flags.REG_NOTEOL) ? 0 : 1];
            ss = miss(css, co, cp);
        /* special case:  match ended at eol? */
            if (ss != null && ss.poststate) {
                return cp;
            } else if (ss != null) {
                ss.setLastSeen(cp); /* to be tidy */
            }
        }

    /* find last match, if any */
        post = -1;
        for (StateSet thisSS : stateSets.values()) { //.object2ObjectEntrySet()) {
            if (thisSS.poststate && post != thisSS.getLastSeen()
                    && (post == -1 || post < thisSS.getLastSeen())) {
                post = thisSS.getLastSeen();
            }
        }
        if (post != -1) {       /* found one */
            /* Post points after the codepoint after the last one in the match (!) */
            /* So, if that is an SMP codepoint, we need to back up 2 to get to the beginning of it,
             * and thus be just after the last character of the match. */
            char postChar = runtime.data.charAt(post - 1);
            if (Character.isLowSurrogate(postChar)) {
                return post - 2;
            } else {
                return post - 1;
            }
        }
        return -1;
    }


    /**
     * shortest - shortest-preferred matching engine
     *
     * @param start   where the match should start
     * @param min     match must end at or after here
     * @param max     match must end at or before here
     * @param coldp   store coldstart pointer here, if non-null. This is the _beginning_ of the match region.
     * @param hitstop record whether hit end of total input
     * @return endpoint or -1
     */
    int shortest(int start, int min, int max, int[] coldp, boolean[] hitstop) {
        int cp;
        int realmin = min == runtime.dataLength ? min : min + 1;
        int realmax = max == runtime.dataLength ? max : max + 1;
        short co;
        StateSet ss;
        StateSet css;

    /* initialize */
        css = initialize(start);
        cp = start;
        if (hitstop != null) {
            hitstop[0] = false;
        }

    /* startup */
        if (cp == 0) {
            /* If the NOTBOL flag is true, we take color as bos[0], else 1. bos[0] is really BOS, while [1] is supposed to be BOL. So, I guess, if it's NOTBOL, it's BOS. */
            /* The combination of NOTBOL and lookingAt is not defined. */
            co = cnfa.bos[0 != (runtime.eflags & Flags.REG_NOTBOL) ? 0 : 1];
        } else {
            /* Not at bos at all, set color based on prior character. */
            char theChar = runtime.data.charAt(cp - 1);
            if (Character.isLowSurrogate(theChar)) {
                int codepoint = Character.toCodePoint(runtime.data.charAt(cp - 2), theChar);
                co = cm.getcolor(codepoint);
            } else {
                co = cm.getcolor(theChar);
            }
        }

        css = miss(css, co, cp);
        if (css == null) {
            return -1;
        }

        css.setLastSeen(cp);
        ss = css;

    /* main loop */
        boolean first = true;
        while (cp < realmax) {
            int increment = 1;
            char theChar = runtime.data.charAt(cp);
            if (Character.isHighSurrogate(theChar)) {
                int codepoint = Character.toCodePoint(theChar,  runtime.data.charAt(cp + 1));
                co = cm.getcolor(codepoint);
                increment = 2;
            } else {
                co = cm.getcolor(theChar);
            }
            ss = css.outs[co];
            if (ss == null) {
                ss = miss(css, co, cp + increment);
                if (ss == null) {
                    break;  /* NOTE BREAK OUT */
                }
            }

            cp = cp + increment;
            ss.setLastSeen(cp);
            css = ss;
            if (ss.poststate && cp >= realmin) {
                break;      /* NOTE BREAK OUT */
            }
            first = false;
        }

        if (ss == null) {
            return -1;
        }

        int matchStart = lastcold();
        if (coldp != null) {    /* report last no-progress state set, if any */
            coldp[0] = matchStart;
        }

        if (ss.poststate && cp > min) {
            assert cp >= realmin;
            cp--;
            if (Character.isLowSurrogate(runtime.data.charAt(cp))) {
                cp--;
            }

        } else if (cp == runtime.dataLength && max == runtime.dataLength) {
            co = cnfa.eos[0 != (runtime.eflags & Flags.REG_NOTEOL) ? 0 : 1];
            ss = miss(css, co, cp);
        /* match might have ended at eol */
            if ((ss == null || !ss.poststate) && hitstop != null) {
                hitstop[0] = true;
            }
        }

        if (ss == null || !ss.poststate) {
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
        int nopr = 0;

        for (StateSet ss : stateSets.values()) {
            if (ss.noprogress && nopr < ss.getLastSeen()) {
                nopr = ss.getLastSeen();
            }
        }
        return nopr;
    }
}

