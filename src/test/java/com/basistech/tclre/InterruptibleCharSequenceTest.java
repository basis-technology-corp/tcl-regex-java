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

import com.basistech.tclre.InterruptedRegexException;
import com.basistech.tclre.InterruptibleCharSequence;
import org.junit.Assert;
import org.junit.Test;

import java.util.regex.Pattern;

/**
 * Tests for {@link com.basistech.tclre.InterruptibleCharSequence}.
 */
public class InterruptibleCharSequenceTest extends Assert {

    private static final String DATA = "This is the cereal shot from guns.";

    @Test
    public void testOffsetChecking() {
        InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
        assertEquals(DATA.length(), seq.length());
        IndexOutOfBoundsException e = null;
        try {
            seq.charAt(DATA.length());
        } catch (IndexOutOfBoundsException iobe) {
            e = iobe;
        }
        assertNotNull(e);

        seq = new InterruptibleCharSequence(DATA.toCharArray(), 5, 10);
        assertEquals(5, seq.length());

        e = null;
        try {
            seq.subSequence(4, 6);
        } catch (IndexOutOfBoundsException iobe) {
            e = iobe;
        }
        assertNotNull(e);
        assertTrue(e.getMessage().contains("Invalid end offset"));

        e = null;
        try {
            seq.subSequence(6, 2);
        } catch (IndexOutOfBoundsException iobe) {
            e = iobe;
        }
        assertNotNull(e);
        assertTrue(e.getMessage().contains("Invalid start offset"));
    }

    @Test
    public void testSubSequence() {
        // this also tests toString(), while we are at it.
        InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
        CharSequence sub = seq.subSequence(1, 4);
        assertEquals("his", sub.toString());
    }

    private static class SelfInterruptingThread extends Thread {

        private final Runnable testCase;
        private Throwable throwable;

        private SelfInterruptingThread(Runnable testCase) {
            this.testCase = testCase;
        }

        @Override
        public void run() {
            // pend an interrupt
            Thread.currentThread().interrupt();

            try {
                testCase.run();
            } catch (Throwable rie) {
                throwable = rie;
            }
        }

        public Throwable getThrowable() {
            return throwable;
        }
    }

    private void checker(Runnable testCase) throws InterruptedException {
        SelfInterruptingThread thread = new SelfInterruptingThread(testCase);
        thread.start();
        thread.join();
        assertTrue(thread.getThrowable() instanceof InterruptedRegexException);
    }

    @Test
    public void testLengthCheck() throws Exception {
        checker(new Runnable() {
            @Override
            public void run() {
                InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
                seq.length(); // get an exception.
            }
        });
    }

    @Test
    public void testCharAtCheck() throws Exception {
        checker(new Runnable() {
            @Override
            public void run() {
                InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
                seq.charAt(3); // get an exception.
            }
        });
    }

    @Test
    public void testSubSequenceCheck() throws Exception {
        checker(new Runnable() {
            @Override
            public void run() {
                InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
                seq.subSequence(1, 4); // get an exception.
            }
        });
    }

    @Test
    public void testtoStringCheck() throws Exception {
        checker(new Runnable() {
            @Override
            public void run() {
                InterruptibleCharSequence seq = new InterruptibleCharSequence(DATA.toCharArray(), 0, DATA.length());
                seq.toString(); // get an exception.
            }
        });
    }

    @Test
    public void pathologicalRegexTest() throws Exception {
        // a case when we may want to use this class: see TEJ-196
        Thread t = new Thread(new Runnable()
            {
                public void run() {
                    String input = "http://en.wikipedia.org/wiki/Athens_(access_and_identity_management_serviceaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa)";
                    InterruptibleCharSequence seq = new InterruptibleCharSequence(input.toCharArray(), 0, input.length());
                    try {
                        Pattern.matches("(?xi)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\(([^\\s()<>]+|(\\([^\\s()>?]+\\)))*\\)))*\\))+(?:\\(([^\\s()<>]+|(\\(([^\\s()<>]+|(\\([^\\s()>?]+\\)))*\\)))*\\)[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))", seq);
                        fail(); // should never reach here
                    } catch(InterruptedRegexException e) {
                        //
                    }
                }
            });
        t.start();
        Thread.sleep(1000);
        t.interrupt();
        t.join();
    }
}
