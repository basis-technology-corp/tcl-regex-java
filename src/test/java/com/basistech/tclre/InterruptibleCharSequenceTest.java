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
