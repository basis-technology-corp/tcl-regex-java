/******************************************************************************
 ** This data and information is proprietary to, and a valuable trade secret
 ** of, Basis Technology Corp.  It is given in confidence by Basis Technology
 ** and may only be used as permitted under the license agreement under which
 ** it has been distributed, and in no other way.
 **
 ** Copyright (c) 2015 Basis Technology Corporation All rights reserved.
 **
 ** The technical data and information provided herein are provided with
 ** `limited rights', and the computer software provided herein is provided
 ** with `restricted rights' as those terms are defined in DAR and ASPR
 ** 7-104.9(a).
 ******************************************************************************/

package com.basistech.tclre;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit testing the color map.
 */
@RunWith(MockitoJUnitRunner.class)
public class ColorMapTest {

    boolean traceNewarc;

    @Mock
    Compiler compiler;

    @Mock
    Nfa nfa;

    private RuntimeColorMap runtime(ColorMap cm) {
        return new RuntimeColorMap(cm.getMap());
    }

    @Test
    public void testEmpty() throws Exception {
        ColorMap cm = new ColorMap(compiler);
        cm.okcolors(nfa);
        assertEquals(Constants.WHITE, runtime(cm).getcolor('a'));
        verify(compiler, never()).getNfa();
        verify(nfa, never()).newarc(anyInt(), anyShort(), (State)anyObject(), (State)anyObject());
    }

    @Test
    public void testVerySimple() throws Exception {
        ColorMap cm = new ColorMap(compiler);
        cm.subcolor('a');
        cm.okcolors(nfa);
        assertEquals(1, cm.subcolor('a'));
        verify(compiler, never()).getNfa();
        verify(nfa, never()).newarc(anyInt(), anyShort(), (State)anyObject(), (State)anyObject());
    }

    @Test
      public void testSimpleRange() throws Exception {
        when(compiler.getNfa()).thenReturn(nfa); // supply the NFA
        ColorMap cm = new ColorMap(compiler);
        State from = Mockito.mock(State.class);
        State to = Mockito.mock(State.class);
        cm.subrange('a', 'z', from, to);

        // we no longer make 26 identical arcs, we make one.
        verify(nfa, times(1)).newarc(Compiler.PLAIN, (short) 1, from, to);

        cm.okcolors(nfa);

        RuntimeColorMap runtime = runtime(cm);
        assertEquals(1, runtime.getcolor('a'));
        assertEquals(1, runtime.getcolor('z'));
        assertEquals(Constants.WHITE, runtime.getcolor('A'));
        verifyNoMoreInteractions(nfa);
    }

    @Test
    public void testSubout() throws Exception {
        when(compiler.getNfa()).thenReturn(nfa); // supply the NFA
        ColorMap cm = new ColorMap(compiler);
        State from = Mockito.mock(State.class);
        State to = Mockito.mock(State.class);
        cm.subrange('a', 'z', from, to);
        InOrder inOrder = inOrder(nfa);

        inOrder.verify(nfa, times(1)).newarc(Compiler.PLAIN, (short) 1, from, to);
        cm.okcolors(nfa);

        cm.subcolor('b');
        cm.okcolors(nfa);
        inOrder.verifyNoMoreInteractions(); // this does not result in more arcs?

        RuntimeColorMap runtime = runtime(cm);
        assertEquals(1, runtime.getcolor('a'));
        assertEquals(2, runtime.getcolor('b'));
        assertEquals(1, runtime.getcolor('z'));
        assertEquals(Constants.WHITE, runtime.getcolor('A'));

    }

    @Test
    public void testSubRange() throws Exception {
        when(compiler.getNfa()).thenReturn(nfa); // supply the NFA
        ColorMap cm = new ColorMap(compiler);
        State from = Mockito.mock(State.class);
        State to = Mockito.mock(State.class);
        cm.subrange('a', 'z', from, to);
        InOrder inOrder = inOrder(nfa);

        inOrder.verify(nfa, times(1)).newarc(Compiler.PLAIN, (short) 1, from, to);
        cm.okcolors(nfa);

        cm.subrange('b', 'c', from, to);
        cm.okcolors(nfa);
        inOrder.verify(nfa, times(1)).newarc(Compiler.PLAIN, (short) 2, from, to);

        RuntimeColorMap runtime = runtime(cm);
        assertEquals(1, runtime.getcolor('a'));
        assertEquals(2, runtime.getcolor('b'));
        assertEquals(1, runtime.getcolor('z'));
        assertEquals(Constants.WHITE, runtime.getcolor('A'));
    }

    @Test
    public void testMoreComplexSubrange() throws Exception {
        when(compiler.getNfa()).thenReturn(nfa); // supply the NFA
        ColorMap cm = new ColorMap(compiler);
        State from = Mockito.mock(State.class);
        State to = Mockito.mock(State.class);
        cm.subrange('a', 'f', from, to);
        cm.okcolors(nfa);
        from = Mockito.mock(State.class);
        to = Mockito.mock(State.class);
        cm.subrange('g', 'z', from, to);
        cm.okcolors(nfa);
        from = Mockito.mock(State.class);
        to = Mockito.mock(State.class);
        cm.subrange('e', 'h', from, to);
        cm.okcolors(nfa);
        Map<Range<Integer>, Short> ranges = cm.getMap().asMapOfRanges();
        assertEquals(6, ranges.size());
        List<Range<Integer>> keys = Lists.newArrayList(ranges.keySet());
        assertEquals(Range.closedOpen(0, (int)'a'), keys.get(0));
        assertEquals(Range.closedOpen((int)'a', (int)'e'), keys.get(1));
        assertEquals(Range.closedOpen((int)'e', (int)'g'), keys.get(2));
        assertEquals(Range.closedOpen((int)'g', (int)'i'), keys.get(3));
        assertEquals(Range.closedOpen((int)'i', (int)'z' + 1), keys.get(4));
    }

    // This proves that, once the dust settles, the colors are what you would expect.
    // It does leave some questions unanswered about the arcs. It seems as if
    // it makes redundant calls to newarc. In the new version, it's not clear how hard
    // to work to avoid this.

    @Test
    public void testBigRange() throws Exception {
        when(compiler.getNfa()).thenReturn(nfa); // supply the NFA

        if (traceNewarc) {
            doAnswer(new Answer() {
                public Object answer(InvocationOnMock invocation) {
                    System.out.printf("%s %s %s %s\n",
                            invocation.getArguments()[0].toString(),
                            invocation.getArguments()[1].toString(),
                            invocation.getArguments()[2],
                            invocation.getArguments()[3]
                    );
                    return null;
                }
            }).when(nfa).newarc(anyInt(), anyShort(), (State) anyObject(), (State) anyObject());
        }

        ColorMap cm = new ColorMap(compiler);
        State from = Mockito.mock(State.class);
        State to = Mockito.mock(State.class);
        cm.subrange('\uc000', '\ue000', from, to);
        cm.okcolors(nfa);

        cm.subrange('\uc001', '\uc101', from, to);
        cm.okcolors(nfa);

        RuntimeColorMap runtime = runtime(cm);
        assertEquals(1, runtime.getcolor('\uc000'));
        assertEquals(2, runtime.getcolor('\uc001'));
        assertEquals(1, runtime.getcolor('\udfff'));
        assertEquals(Constants.WHITE, runtime.getcolor('A'));
    }
}
