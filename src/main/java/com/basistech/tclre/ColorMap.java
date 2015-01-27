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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.BoundType;
import com.google.common.collect.Lists;

import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;
import com.ibm.icu.lang.UCharacter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the assignment of colors for characters. Arcs are labelled with colors, which group characters.
 * code from regc_color.c.

 * What do we know:
 * When the code is not busy processing a [] or subexpression of some kind, the 'tree' in here is a map from characters to colors.
 * Initially, all characters are color 0 (WHITE). When the compiler needs a color for an arc, it calls 'newsub'.
 *
 * Think in terms of an 'epoch'. An epoch is the time from one call to okcolors to the next. (There's an implicit call to okcolors at
 * the start of a compilation.) okcolors is called at the end of processing an atom; a range, a subexpression, or an ordinary item
 * such as a character not in a range.
 *
 * When the compiler sees a character or a range during an epoch, it calls 'subcolor' or 'subrange'. This asks for a new color to be allocated to the
 * character or to each of a range of characters.
 * <pre>
 * - It gets the ColorDesc for the color currently assigned to the character.
 * - It allocates a new color desc and color number.
 * - It fills in the new color number as the 'sub' of the old color.
 * - It returns the sub color number to the rest of the compiler.
 * </pre>
 *
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
 * There's a wad of complexity in here related to blocks; if a range is large enough to span a 'block' the code works to align colors to blocks.
 * I'm thinking this is all expendable.
 *
 * As colors are promoted, the nfa gets new arcs. It does not appear to lose the old arcs; I suspect that the optimization process somehow
 * removes them.
 */
class ColorMap {
    static final Logger LOG = LoggerFactory.getLogger(ColorMap.class);
    private final short[] map;
    // this is called 'v' in the C code.
    private Compiler compiler; // for compile error reporting
    private final List<ColorDesc> colorDescs; // all the color descs. A list for resizability.

    ColorMap(Compiler compiler) {
        map = new short[Character.MAX_VALUE + 1];
        Arrays.fill(map, Constants.WHITE);
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
    private short getcolor(char c) {
        return map[c]; // chars are unsigned.
    }

    /**
     * setcolor - set the color of a character in a colormap
     */
    private short setcolor(char c, short co) {
        short prev = map[c];
        map[c] = co;
        return prev;
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
     * This is the only API that allocates colors. Compiler calls here to assign a color
     * to a character.
     */
    short subcolor(char c) throws RegexException {
        short co;           /* current color of c */
        short sco;          /* new subcolor */

        co = getcolor(c);
        sco = newsub(co);
        assert sco != Constants.COLORLESS;

        if (co == sco)      /* already in an open subcolor */ {
            return co;  /* rest is redundant */
        }

        ColorDesc cd = colorDescs.get(co);
        cd.incrementNChars(-1);
        ColorDesc scd = colorDescs.get(sco);
        scd.incrementNChars(1);
        setcolor(c, sco);
        return sco;
    }

    /**
     * newsub - allocate a new subcolor (if necessary) for a color
     */
    private short newsub(short co) throws RegexException {
        short sco; // new subclolor.

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
     */
    void subrange(char from, char to, State lp, State rp) throws RegexException {
        /*
         * For each char in the range, acquire a subcolor and make the arc.
         * Note that if the new range is a subset of an old range, they will all get the
         * same subcolor.
         */
        short prevColor = -1;
        for (char ch = from; ch <= to; ch++) {
            short color = subcolor(ch);
            if (color != prevColor) {
                compiler.getNfa().newarc(Compiler.PLAIN, color, lp, rp);
                prevColor = color;
            }
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
    short[] getMap() {
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
        RangeSet<Character> rangeSet = TreeRangeSet.create();
        int start = 0;
        while(start <= 0xffff) {
            if (map[start] == co) {
                int end;
                for (end = start + 1; end <= 0xffff && map[end] == co; end++) {
                    //
                }
                // end is one past end of range, so ...
                if (end <= Character.MAX_VALUE) {
                    rangeSet.add(Range.closedOpen((char) start, (char) end));
                } else {
                    rangeSet.add(Range.closed((char)start, Character.MAX_VALUE));
                }
                start = end;
            } else {
                start++;
            }
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.format("Color %d - %d chars %s\n", co, cd.getNChars(), cd.pseudo() ? " (pseudo)" : "");
        for (Range<Character> range : rangeSet.asRanges()) {
            Character lower = range.lowerEndpoint();
            if (range.lowerBoundType() == BoundType.OPEN) {
                lower++;
            }
            Character upper = range.upperEndpoint();
            if (range.upperBoundType() == BoundType.OPEN) {
                upper--;
            }
            if (lower == upper) {
                pw.format(" %s (U+%04x)\n", UCharacter.getExtendedName(lower), (int)lower);
            } else {
                pw.format(" U+%04x - U+%04x (%s - %s)\n", (int)lower, (int) upper, UCharacter.getExtendedName(lower), UCharacter.getExtendedName(upper));
            }
        }
        pw.flush();
        LOG.debug(sw.toString());
    }
}
