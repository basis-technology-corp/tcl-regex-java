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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class NfaTest extends Assert {

    private ColorMap mockColorMap() {
        ColorMap colorMap = Mockito.mock(ColorMap.class);
        Mockito.when(colorMap.subcolor('a')).thenReturn((short)1);
        return colorMap;
    }

    @Test
    public void newarc() {
        Nfa nfa = new Nfa(mockColorMap());
        short color = nfa.cm.subcolor('a');
        State s1 = nfa.newState();
        State s2 = nfa.newState();
        assertNotNull(s1);
        assertNotNull(s2);
        nfa.newarc(Compiler.PLAIN, color, s1, s2);
        assertEquals(1, s1.nouts);
        assertEquals(1, s2.nins);
        assertEquals(2, nfa.nstates);
        assertSame(s1, nfa.states);
        assertSame(s2, s1.next);
        assertSame(s1, s2.prev);

        Arc a = s1.outs;
        assertSame(s1, a.from);
        assertSame(s2, a.to);
        assertEquals(color, a.co);
        assertTrue(a.colored());
    }

    @Test
    public void dropstate() {
        Nfa nfa = new Nfa(mockColorMap());
        State s1 = nfa.newState();
        nfa.dropstate(s1);
        assertEquals(0, nfa.nstates);
        assertNull(nfa.states);
    }
}
