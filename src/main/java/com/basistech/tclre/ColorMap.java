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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;

import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import com.ibm.icu.lang.UCharacter;

/**
 * Manage the assignment of colors for characters. Arcs are labelled with colors, which group characters.
 * Original code was in regc_color.c.
 * <p/>
 * At any point in time, all possible characters are assigned a color.
 * Initially, all characters are color 0 (WHITE). Characters get other colors when the compiler encounters them
 * in an expression. The compiler processes characters two ways: either as single characters,
 * or in ranges. When the compiler sees a single character in isolation, it calls {@link #subcolor(int)}.
 * When it processes a range, it calls {@link #subrange}.
 * <p/>
 * This class expects the compiler to call {@link #okcolors} at the end of each 'atom'. An atom is character in isolation, a range, or a subexpression.
 * <p/>
 * During the period from one call to {@link #okcolors} to the next, this class can maintain an open 'subcolor' for each color.
 * The idea here is that each new item moves some characters from the color they have at the beginning of the period to a new color.
 * One period is always connecting two states with one or more arcs. If all the characters have the same color, there is one arc;
 * if the characters end up with disparate colors, it is multiple arcs. In a single period, all the characters that are moved out of
 * a particular color have to move into the same new color. Thus, the process of allocating a subcolor works like:
 *
 * <pre>
 * - It gets the ColorDesc for the color currently assigned to the character.
 * - It allocates a new color desc and color number.
 * - It fills in the new color number as the 'sub' of the old color.
 * - It returns the sub color number to the rest of the compiler.
 * </pre>
 *
 * Subsequent calls to subcolor in an epoch for other characters with the same current color get the same subcolor.
 * Note that in the subrange case, it will call newsub for each item, but once a color has a subcolor, it reuses it, so all the other characters
 * in the range end up (sub)colored identically.
 *
 * So, for an expression like [a-z]ab, we start with color 0 owning everything. When we hit the range, we allocate subcolors for all of a-z -- but they
 * all get the same subcolor. At the ']', okcolors ends the epoch, and 'promotes' the subcolors to colors. Then it processes 'a'. Now a is in color 1,
 * which gets a subcolor of 2. At the end of the epoch, that promotes, and now a is color 2 and color 1 has no subcolor. The same thing
 * happens to 'b', giving it yet another color split from color 1.
 *
 * As colors are promoted, the nfa gets new arcs. It does not appear to lose the old arcs; I suspect that the optimization process somehow
 * removes them.
 */
class ColorMap {
    private final RangeMap<Integer, Short> map;
    // this is called 'v' in the C code.
    private Compiler compiler; // for compile error reporting
    private final List<ColorDesc> colorDescs; // all the color descs. A list for resizability.

    ColorMap(Compiler compiler) {
        map = TreeRangeMap.create();
        // the color map starts by assigning all characters to WHITE
        map.put(Range.closed(0, Character.MAX_CODE_POINT), Constants.WHITE);
        this.compiler = compiler;

        colorDescs = Lists.newArrayList();

        ColorDesc white = new ColorDesc(); // [WHITE]
        colorDescs.add(white);
        assert colorDescs.size() == 1;
        white.sub = Constants.NOSUB;
        white.setNChars(65536);
    }

    /**
     * Retrieve the color for a character.
     * @param c input char.
     * @return output color.
     */
    private short getcolor(int c) {
        try {
            return map.get(c);
        } catch (NullPointerException npe) {
            throw new RegexRuntimeException(String.format("Failed to map codepoint U+%08X.", c));
        }
    }

    /**
     * Maximum valid color, which might encompass some free colors.
     *
     * @return
     */
    short maxcolor() {
        return (short)(colorDescs.size() - 1);
    }

    private short newcolor() {
        short colorIndex = -1;
        for (short x = 0; x < colorDescs.size(); x++) {
            if (colorDescs.get(x) == null) {
                colorIndex = x;
                break;
            }
        }

        ColorDesc newcd = new ColorDesc();
        if (colorIndex == -1) {
            colorIndex = (short)colorDescs.size();
            colorDescs.add(newcd);
        } else {
            colorDescs.set(colorIndex, newcd);
        }
        return colorIndex;
    }

    private void freecolor(short co) {
        assert co >= 0;
        if (co == Constants.WHITE) {
            return;
        }

        // if this is the very last one, shrink
        if (co == colorDescs.size() - 1) {
            colorDescs.remove(co); // just shrink the List.
            return;
        }

        colorDescs.set(co, null);
    }

    /**
     * pseudocolor - allocate a false color to be managed by other means.
     *
     * @return a color, otherwise unused.
     */
    short pseudocolor() {
        short co = newcolor();
        ColorDesc cd = colorDescs.get(co);
        cd.setNChars(1);
        cd.markPseudo();
        return co;
    }

    /**
     * subcolor - allocate a new subcolor (if necessary) to this char
     * Internal API that can do a range of characters; called from
     * {@link #subrange}.
     *
     * @param c The character, or first character in a range, to process.
     * @param rangeCount the number of characters.
     */
    private short subcolor(int c, int rangeCount) throws RegexException {
        short co;           /* current color of c */
        short sco;          /* new subcolor */

        co = getcolor(c);
        sco = newsub(co);
        assert sco != Constants.COLORLESS;

        if (co == sco)      /* already in an open subcolor */ {
            return co;  /* rest is redundant */
        }

        ColorDesc cd = colorDescs.get(co);
        cd.incrementNChars(-rangeCount);
        ColorDesc scd = colorDescs.get(sco);
        scd.incrementNChars(rangeCount);

        map.put(Range.closedOpen(c, c + rangeCount), sco);
        return sco;
    }

    /**
     * Allocate a color for one character. In the range case, call {@link #subrange}.
     * @param c the character
     * @return the subcolor
     * @throws RegexException
     */
    short subcolor(int c) throws RegexException {
        return subcolor(c, 1);
    }

    /**
     * newsub - allocate a new subcolor (if necessary) for a color
     */
    private short newsub(short co) throws RegexException {
        short sco; // new subcolor.

        ColorDesc cd = colorDescs.get(co);

        sco = colorDescs.get(co).sub;
        if (sco == Constants.NOSUB) {       /* color has no open subcolor */
            if (cd.getNChars() == 1) { /* optimization */
                return co;
            }
            sco = newcolor();   /* must create subcolor */
            if (sco == Constants.COLORLESS) {
                throw new RegexException("Invalid color allocation");
            }

            ColorDesc subcd = colorDescs.get(sco);
            cd.sub = sco;
            subcd.sub = sco;    /* open subcolor points to self */
        }

        return sco;
    }

    /**
     * subrange - allocate new subcolors to this range of chars, fill in arcs.
     * The range will overlap existing ranges; even in the simplest case,
     * it will overlap the initial WHITE range. For each existing range that
     * it overlaps, allocate a new color, mark the range as mapping to that color,
     * and add an arc between the states for that color.
     */
    void subrange(int from, int to, State lp, State rp) throws RegexException {
        /* Avoid one call to map.get() for each character in the range.
         * This map will usually contain one item, but in complex cases more.
         * For example, if we had [a-f][g-h] and then someone asked for [f-g], there
         * would be two. Each of these new ranges will get a new color via subcolor.
         */
        Map<Range<Integer>, Short> curColors = map.subRangeMap(Range.closed(from, to)).asMapOfRanges();
        /*
         * To avoid concurrent mod problems, we need to copy the ranges we are working from.
         */
        List<Range<Integer>> ranges = Lists.newArrayList(curColors.keySet());
        for (Range<Integer> rangeToProcess : ranges) {
            // bound management here irritating.
            int start = rangeToProcess.lowerEndpoint();
            if (rangeToProcess.lowerBoundType() == BoundType.OPEN) {
                start++;
            }
            int end = rangeToProcess.upperEndpoint();
            if (rangeToProcess.upperBoundType() == BoundType.CLOSED) {
                end++;
            }
            // allocate a new subcolor and account it owning the entire range.
            short color = subcolor(start, end - start);
            compiler.getNfa().newarc(Compiler.PLAIN, color, lp, rp);
        }
    }

    /**
     * okcolors - promote subcolors to full colors
     */
    void okcolors(Nfa nfa) {
        ColorDesc cd;
        ColorDesc scd;
        Arc a;
        short sco;

        for (short co = 0; co < colorDescs.size(); co++) {
            cd = colorDescs.get(co);
            if (cd == null) {
                continue; // not in use at all, so can't have a subcolor.
            }

            sco = cd.sub;

            if (sco == Constants.NOSUB) {
            /* has no subcolor, no further action */
            } else if (sco == co) {
            /* is subcolor, let parent deal with it */
            } else if (cd.getNChars() == 0) {
            /* parent empty, its arcs change color to subcolor */
                cd.sub = Constants.NOSUB;
                scd = colorDescs.get(sco);

                assert scd.getNChars() > 0;
                assert scd.sub == sco;

                scd.sub = Constants.NOSUB;
                while ((a = cd.arcs) != null) {
                    assert a.co == co;
                    cd.arcs = a.colorchain;
                    a.setColor(sco);
                    a.colorchain = scd.arcs;
                    scd.arcs = a;
                }
                freecolor(co);
            } else {
                /* parent's arcs must gain parallel subcolor arcs */
                cd.sub = Constants.NOSUB;
                scd = colorDescs.get(sco);

                assert scd.getNChars() > 0;
                assert scd.sub == sco;

                scd.sub = Constants.NOSUB;

                for (a = cd.arcs; a != null; a = a.colorchain) {
                    assert a.co == co;
                    nfa.newarc(a.type, sco, a.from, a.to);
                }
            }
        }
    }

    /**
     * colorchain - add this arc to the color chain of its color
     */
    void colorchain(Arc a) {
        ColorDesc cd = colorDescs.get(a.co);
        a.colorchain = cd.arcs;
        cd.arcs = a;
    }

    /**
     * uncolorchain - delete this arc from the color chain of its color
     */
    void uncolorchain(Arc a) {
        ColorDesc cd = colorDescs.get(a.co);
        Arc aa;

        aa = cd.arcs;
        if (aa == a) {      /* easy case */
            cd.arcs = a.colorchain;
        } else {
            for (; aa != null && aa.colorchain != a; aa = aa.colorchain) {
                //
            }
            assert aa != null;
            aa.colorchain = a.colorchain;
        }

        a.colorchain = null;    /* paranoia */
    }

    /**
     * rainbow - add arcs of all full colors (but one) between specified states
     *
     * @param but is COLORLESS if no exceptions
     */
    void rainbow(Nfa nfa, int type, short but, State from, State to) {
        ColorDesc cd;
        short co;

        for (co = 0; co < colorDescs.size(); co++) {
            cd = colorDescs.get(co);
            if (cd != null
                    && cd.sub != co
                    && co != but
                    && !cd.pseudo()) {
                nfa.newarc(type, co, from, to);
            }
        }
    }

    /**
     * colorcomplement - add arcs of complementary colors
     * The calling sequence ought to be reconciled with cloneouts().
     *
     * @param of complements of this guy's PLAIN outarcs
     */
    void colorcomplement(Nfa nfa, int type, State of, State from, State to) {
        ColorDesc cd;
        short co;

        assert of != from;
        for (co = 0; co < colorDescs.size(); co++) {
            cd = colorDescs.get(co);
            if (cd != null && !cd.pseudo()) {
                if (of.findarc(Compiler.PLAIN, co) == null) {
                    nfa.newarc(type, co, from, to);
                }
            }
        }
    }

    /**
     * Return the map for use in the runtime.
     * @return the map.
     */
    RangeMap<Integer, Short> getMap() {
        return map;
    }

    /**
     * dumpcolors - debugging output
     */
    void dumpcolors() {
        /*
         * we want to organize this by colors.
         */
        for (int co = 0; co < colorDescs.size(); co++) {
            ColorDesc cd = colorDescs.get(co);
            if (cd != null) {
                dumpcolor(co, cd);
            }
        }
    }

    /*
     * Not speedy. This is for debugging.
     */
    private void dumpcolor(int co, ColorDesc cd) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.format("Color %d - %d chars %s\n", co, cd.getNChars(), cd.pseudo() ? " (pseudo)" : "");
        for (Map.Entry<Range<Integer>, Short> me : map.asMapOfRanges().entrySet()) {
            if (me.getValue() == co) {
                pw.format(" %s %s\n", me.getKey(), UCharacter.getExtendedName(me.getKey().lowerEndpoint()));
            }
        }
        pw.flush();
        String r = sw.toString();
        System.out.println(r);
    }
}
