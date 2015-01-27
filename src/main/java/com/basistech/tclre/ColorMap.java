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

import java.util.List;

import com.google.common.collect.Lists;

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
    final Tree[] tree;
    // this is called 'v' in the C code.
    Compiler compiler; // for compile error reporting
    int free; // beginning of free chain (if non-zero)
    List<ColorDesc> colorDescs; // all the color descs. A list for resizability.

    ColorMap(Compiler compiler) {
        tree = buildTree();
        this.compiler = compiler;

        free = -1;
        colorDescs = Lists.newArrayList();

        // reg_color.c: initcm.

        ColorDesc white = new ColorDesc(); // [WHITE]
        colorDescs.add(white);
        assert colorDescs.size() == 1;
        white.sub = Constants.NOSUB;
        white.setNChars(65536);
        white.block = tree[Constants.NBYTS - 1];
    }

    private static Tree[] buildTree() {
        Tree[] tree = new Tree[Constants.NBYTS];
        // allocate top-level array of tree objects.
        for (int tx = 0; tx < tree.length; tx++) {
            tree[tx] = new Tree();
        }
        // Make each top-level's collection of next-level pointers point to the next item
        // at the top level.
        for (int tx = 0; tx < tree.length - 1; tx++) {
            Tree t = tree[tx];
            Tree nextT = tree[tx + 1];
            for (int i = Constants.BYTTAB - 1; i >= 0; i--) {
                t.ptrs[i] = nextT;
            }
        }

        // bottom level is solid white.
        Tree t = tree[Constants.NBYTS - 1];
        for (int i = Constants.BYTTAB - 1; i >= 0; i--) {
            t.ccolor[i] = Constants.WHITE;
        }

        return tree;
    }

    private static short b0(char c) {
        return (short)(c & Constants.BYTMASK);
    }

    private static short b1(char c) {
        return (short)((c >>> Constants.BYTBITS) & Constants.BYTMASK);
    }

    /**
     * Retrieve the color for a character.
     * @param c input char.
     * @return output color.
     */
    private short getcolor(char c) {
        // take the first tree item in the map, then go down two levels.
        // why the extra level?
        return tree[0].ptrs[b1(c)].ccolor[b0(c)];
    }

    /**
     * setcolor - set the color of a character in a colormap
     */
    private short setcolor(char c, short co) {
        char uc = c;
        int b;

        if (co == Constants.COLORLESS) {
            return Constants.COLORLESS;
        }

        Tree t = tree[0];
        Tree lastt;
        for (int level = 0, shift = Constants.BYTBITS * (Constants.NBYTS - 1);
                shift > 0;
                level++, shift -= Constants.BYTBITS) {
            b = (uc >>> shift) & Constants.BYTMASK;
            lastt = t;
            t = lastt.ptrs[b];
            assert t != null;

            Tree fillt = tree[level + 1];
            boolean bottom = shift <= Constants.BYTBITS;
            Tree cb = bottom ? colorDescs.get(t.ccolor[0]).block : fillt;
            if (t == fillt || t == cb) {    /* must allocate a new block */
                Tree newt = new Tree();
                if (bottom) {
                    newt.ccolor = t.ccolor.clone();
                } else {
                    newt.ptrs = t.ptrs.clone();
                }
                t = newt;
                lastt.ptrs[b] = t;
            }
        }

        b = uc & Constants.BYTMASK;
        short prev = t.ccolor[b];
        t.ccolor[b] = (short)co;
        return prev;
    }

    /*
     * The C code goes to lengths to compress the array of
     * color descs in various ways. This code preserves the
     * concept of a color value as an index into the array,
     * but takes a simpler approach to storage.
     */

    /**
     * Maximum valid color, which might encompass some free colors.
     *
     * @return
     */
    short maxcolor() {
        return (short)(colorDescs.size() - 1);
    }

    private short newcolor() {
        if (free != -1) {
            assert free > 0; // slot 0 can't be free.
            int toReturn = free;
            ColorDesc cd = colorDescs.get(toReturn);
            assert cd.unusedColor();
            assert cd.arcs == null;
            free = cd.free;
            cd.reset();
            return (short)toReturn;
        } else {
            ColorDesc newcd = new ColorDesc();
            int colorIndex = colorDescs.size();
            colorDescs.add(newcd);
            assert colorIndex != -1;
            return (short)colorIndex;
        }
    }

    private void freecolor(short co) {
        assert co >= 0;
        if (co == Constants.WHITE) {
            return;
        }

        // if this is the very last one, don't bother with a free list!
        if (co == colorDescs.size() - 1) {
            colorDescs.remove(co); // just shrink the List.
            return;
        }

        // if not the last one, participate in the free list.
        ColorDesc cd = colorDescs.get(co);
        assert cd.arcs == null;
        assert cd.sub == Constants.NOSUB;
        assert cd.getNChars() == 0;
        cd.markFree();

        if (free != -1) {
            final ColorDesc colorDesc = colorDescs.get(free);
            colorDesc.free = co;
        }
        free = co;
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
        cd.flags = ColorDesc.PSEUDO;
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
     * subrange - allocate new subcolors to this range of chrs, fill in arcs
     */
    void subrange(char from, char to, State lp, State rp) throws RegexException {
        char uf;
        int i;

        assert from <= to;

    /* first, align "from" on a tree-block boundary */
        uf = from;
        i = ((uf + Constants.BYTTAB - 1) & (char)~Constants.BYTMASK) - uf;
        for (; from <= to && i > 0; i--, from++) {
            compiler.nfa.newarc(Compiler.PLAIN, subcolor(from), lp, rp);
        }

        if (from > to) {            /* didn't reach a boundary */
            return;
        }

        /* deal with whole blocks */
        for (; to - from >= Constants.BYTTAB; from += Constants.BYTTAB) {
            subblock(from, lp, rp);
        }

    /* clean up any remaining partial table */
        for (; from <= to; from++) {
            compiler.nfa.newarc(Compiler.PLAIN, subcolor(from), lp, rp);
        }
    }

    /**
     * subblock - allocate new subcolors for one tree block of chars, fill in arcs
     */
    private void subblock(final char start, final State lp, final State rp) throws RegexException {
        char uc = start;
        int shift;
        int level;
        int i;
        int b = -1; // Java is not sure this gets initialized; find out for sure.
        Tree t;
        Tree cb;
        Tree fillt;
        Tree lastt = null;
        int previ;
        int ndone;
        short co;
        short sco;

        assert (uc % Constants.BYTTAB) == 0;

    /* find its color block, making new pointer blocks as needed */
        t = tree[0]; //
        fillt = null;
        for (level = 0, shift = Constants.BYTBITS * (Constants.NBYTS - 1); shift > 0;
                level++, shift -= Constants.BYTBITS) {
            b = (uc >>> shift) & Constants.BYTMASK;
            lastt = t;
            t = lastt.ptrs[b];

            assert t != null;
            fillt = tree[level + 1];
            if (t == fillt && shift > Constants.BYTBITS) {  /* need new ptr block */
                t = new Tree();
                t.ptrs = fillt.ptrs.clone();
                lastt.ptrs[b] = t;
            }
        }

    /* special cases:  fill block or solid block */
        co = t.ccolor[0];
        ColorDesc cd = colorDescs.get(co);
        cb = cd.block;
        if (t == fillt || t == cb) {
        /* either way, we want a subcolor solid block */
            sco = newsub(co);
            ColorDesc scd = colorDescs.get(sco);
            t = scd.block;
            if (t == null) {    /* must set it up */
                t = new Tree();
                for (i = 0; i < Constants.BYTTAB; i++) {
                    t.ccolor[i] = sco;
                }
                scd.block = t;
            }
        /* find loop must have run at least once */
            lastt.ptrs[b] = t;

            compiler.nfa.newarc(Compiler.PLAIN, sco, lp, rp);
            cd.incrementNChars(Constants.BYTTAB);
            scd.incrementNChars(Constants.BYTTAB);
            return;
        }

    /* general case, a mixed block to be altered */
        i = 0;
        while (i < Constants.BYTTAB) {
            co = t.ccolor[i];
            cd = colorDescs.get(co);
            sco = newsub(co);
            ColorDesc scd = colorDescs.get(sco);
            compiler.nfa.newarc(Compiler.PLAIN, sco, lp, rp);
            previ = i;
            do {
                t.ccolor[i++] = sco;
            } while (i < Constants.BYTTAB && t.ccolor[i] == co);
            ndone = i - previ;
            cd.incrementNChars(-ndone);
            scd.incrementNChars(ndone);
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
            sco = cd.sub;

            if (cd.unusedColor() || sco == Constants.NOSUB) {
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
            if (!cd.unusedColor()
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
            if (!cd.unusedColor() && !cd.pseudo()) {
                if (of.findarc(Compiler.PLAIN, co) == null) {
                    nfa.newarc(type, co, from, to);
                }
            }
        }
    }

    /**
     * dumpcolors - debugging output
     */
    void dumpcolors() {
        ColorDesc cd;
        short co;
        char c;
        String has;

        if (LOG.isDebugEnabled()) {
            LOG.debug("colorDescs.size() {}", colorDescs.size());
        }
        fillcheck(0);

        for (co = 1; co < colorDescs.size(); co++) {
            cd = colorDescs.get(co);
            if (!cd.unusedColor()) {
                assert cd.getNChars() > 0;
                has = cd.block != null ? "#" : "";
                StringBuilder msg = new StringBuilder();
                if (cd.pseudo()) {
                    msg.append(String.format("#%2d%s(pseudo): ", (int)co, has));
                } else {
                    msg.append(String.format("#%2d%s(%d): ", (int)co, has, cd.getNChars()));
                }
                /* it's hard to do this more efficiently */
                /* these can get large ... */
                char startRange = 0xffff;
                int rangeCount = 0;
                for (c = Constants.CHR_MIN; c < Constants.CHR_MAX; c++) {
                    if (getcolor(c) == co) {
                        if (c == startRange + rangeCount) { // does it extend the range?
                            rangeCount++;
                        } else if (startRange != (char)0xffff) {
                            if (rangeCount == 0) {
                                msg.append(dumpchr(startRange));
                            } else {
                                dumpMapRange(msg, startRange, rangeCount);
                            }
                            startRange = c;
                            rangeCount = 1;
                        } else {
                            startRange = c; // first attempt at a range.
                            rangeCount = 1;
                        }
                    }
                }
                if (rangeCount != 0) {
                    dumpMapRange(msg, startRange, rangeCount);
                }

                assert c == Constants.CHR_MAX;
                if (getcolor(c) == co) {
                    msg.append(dumpchr(c));
                }
                LOG.debug(msg.toString());
            }
        }
    }

    private void dumpMapRange(StringBuilder msg, char startRange, int rangeCount) {
        if (rangeCount == 1) {
            msg.append(dumpchr(startRange));
        } else {
            msg.append(String.format("[%s-%s]", dumpchr(startRange),
                    dumpchr((char)(startRange + rangeCount - 1))));
        }
    }

    /**
     * - fillcheck - check proper filling of a tree
     */
    void fillcheck(int level) {
        int i;
        Tree t;
        Tree fillt = tree[level + 1];

        assert level < Constants.NBYTS - 1; /* this level has pointers */

        for (i = Constants.BYTTAB - 1; i >= 0; i--) {
            t = tree[0].ptrs[i];
            if (t == null) {
                // this might not be an error, it was just a debug message in c.
                throw new RuntimeException("null in filled color tree");
            } else if (t == fillt) {
                // do nothing
            } else if (level < Constants.NBYTS - 2) /* more pointer blocks below */ {
                fillcheck(level + 1);
            }
        }
    }

    /**
     * dumpchr - print a chr
     * Kind of char-centric but works well enough for debug use.
     */

    String dumpchr(char c) {
        if (c == '\\') {
            return "\\\\";
        } else if (c > ' ' && c <= '~') {
            return Character.toString(c);
        } else {
            return String.format("\\u%04x", (int)c);
        }
    }

    /**
     * Color tree
     */
    /* C unions this. We use more memory instead. See TODO
     comments at top of file as to whether this all makes sense.
     */
    static class Tree {
        short[] ccolor = new short[Constants.BYTTAB];
        Tree[] ptrs = new Tree[Constants.BYTTAB];
    }

}
