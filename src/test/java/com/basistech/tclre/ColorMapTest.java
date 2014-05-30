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

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * Unit tests for the color map management.
 * Because I (benson) don't claim to completely understand this code, it may initially be
 * primarily an exercise in trying to provoke stupid crashes.
 */
public class ColorMapTest extends Assert {

    static final char TESTCHAR1 = '\u4e00';
    static final char TESTCHAR2 = '\u4e01';
    static final char TESTCHAR3 = '\u4e02';

    private Compiler mockCompiler() {
        Compiler compiler = Mockito.mock(Compiler.class);
        //TODO: fill in what to do.
        return compiler;
    }

    @Test
    public void initialSetup() {
        ColorMap cm = new ColorMap(mockCompiler());

        assertFalse(cm.isErr());
        assertEquals(0, cm.free);

        ColorDesc white = cm.colorDescs.get(Constants.WHITE);
        assertNotNull(white);
        // all chars should start out as white?
        for (int c = 0; c <= 0xffff; c += 0x1000) {
            assertEquals(Constants.WHITE, cm.getcolor((char)c));
        }

        assertEquals(Constants.WHITE, cm.getcolor('\uffff'));
    }

    private void assertColorReset(ColorDesc cd) {
        assertEquals(0, cd.nchrs);
        assertEquals(-1, cd.sub);
        assertNull(cd.arcs);
        assertEquals(0, cd.flags);
        assertNull(cd.block);
    }

    @Test
    public void simpleColorAllocation() {
        ColorMap cm = new ColorMap(mockCompiler());
        short newColor = cm.newcolor();
        assertNotEquals(Constants.WHITE, newColor);
        assertColorReset(cm.colorDescs.get(newColor));
    }

    @Test
    public void colorAllocFreeEnd() {
        ColorMap cm = new ColorMap(mockCompiler());
        short newColor = cm.newcolor();
        // freeing last color.
        cm.freecolor(newColor);
        // trimmed the array?
        assertEquals(1, cm.colorDescs.size());
        // did not set the free link.
        assertEquals(0, cm.free);
    }

    @Test
    public void colorAllocFreeMiddle() {
        ColorMap cm = new ColorMap(mockCompiler());
        short newColor1 = cm.newcolor();
        cm.newcolor(); // allocate a second.
        cm.freecolor(newColor1);
        // Not trimmed the array?
        assertEquals(3, cm.colorDescs.size());
        // did set the free link.
        assertEquals(1, cm.free);
        assertEquals(ColorDesc.FREECOL, cm.colorDescs.get(1).flags);
    }

    // Note that setcolor is entirely internal to ColorMap; it does not set nchrs to 1.
    // All callers call subcolor. This is just a test of low-level book-keeping.
    @Test
    public void setcolor() {
        ColorMap cm = new ColorMap(mockCompiler());
        short newColor = cm.newcolor();
        short oldColor = cm.setcolor(TESTCHAR1, newColor);
        assertEquals(Constants.WHITE, oldColor);
        assertNotSame(Constants.WHITE, newColor);
        assertEquals(newColor, cm.getcolor(TESTCHAR1));
        // Make sure we didn't bust something else.
        for (int c = 0; c <= 0xffff; c += 0x1000) {
            if (c != 0x4e00) {
                assertEquals(Constants.WHITE, cm.getcolor((char) c));
            }
        }
        assertEquals(Constants.WHITE, cm.getcolor('\uffff'));
    }

    @Test
    public void subcolor() {

        ColorMap cm = new ColorMap(mockCompiler());
        short color1 = cm.subcolor(TESTCHAR1);
        assertNotSame(Constants.WHITE, color1);
        ColorDesc cd1 = cm.colorDescs.get(color1);
        assertEquals(1, cd1.nchrs);
        assertEquals(color1, cm.getcolor(TESTCHAR1));

        // the subcolor is open, so it ends up in the same place.
        short color2 = cm.subcolor(TESTCHAR2);
        assertEquals(color1, color2);
        assertEquals(color2, cm.getcolor(TESTCHAR2));
        // we have no arcs, so a catatonic mock is enough mock.
        Nfa nfa = Mockito.mock(Nfa.class);
        cm.okcolors(nfa);
        short color3 = cm.subcolor(TESTCHAR3);
        assertNotSame(color2, color3);
    }

    @Test
    public void colorchain() {
        ColorMap cm = new ColorMap(mockCompiler());
        // we need a mimimal NFA. Mocking does not help, we need actual data structure.
        Nfa nfa = new Nfa(cm);
        State s0 = nfa.newState();
        State s1 = nfa.newState();
        State s2 = nfa.newState();
        State s3 = nfa.newState();

        short color1 = cm.subcolor(TESTCHAR1);

        nfa.newarc(Compiler.PLAIN, color1, s0, s1);
        nfa.newarc(Compiler.PLAIN, color1, s0, s3);
        cm.okcolors(nfa);

        short color2 = cm.subcolor(TESTCHAR2);
        nfa.newarc(Compiler.PLAIN, color2, s2, s3);
        cm.okcolors(nfa);

        ColorDesc cd = cm.colorDescs.get(color1);

        assertNotNull(cd.arcs.colorchain);
        List<Arc> color1Arcs = collectArcList(cd.arcs);
        assertEquals(2, color1Arcs.size());
        for (Arc a : color1Arcs) {
            // all color1 arcs start with s0
            assertEquals(color1, a.co);
            assertSame(s0, a.from);
        }

        Arc a = s1.ins; // take the S0->S1 arc

        cm.uncolorchain(cd.arcs);
        assertNotNull(cd.arcs); // leave the prior one.
        assertNull(a.colorchain);
    }

    // collects the arcs from a colorchain
    private List<Arc> collectArcList(Arc arcs) {
        List<Arc> list = Lists.newArrayList();
        for (Arc a = arcs; a != null; a = a.colorchain) {
            list.add(a);
        }
        return list;
    }
}
