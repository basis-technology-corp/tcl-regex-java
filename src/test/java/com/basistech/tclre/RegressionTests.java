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

import java.util.EnumSet;

import org.junit.Test;
import com.basistech.rosette.util.InterruptibleCharSequence;

/**
 * Home for tests that result from problems seen 'in the wild'.
 */
public class RegressionTests extends Utils {

    @Test
    public void nestedNestAssertion() throws Exception {
        // blew up with assert in lexnest.
        String boom = "(?i)\\m(?:(?:[\\+\\-]?\\d{1,3}))";
        HsrePattern.compile(boom, PatternFlags.ADVANCED);
    }

    @Test
    public void testConfusedBrackets() throws Exception {
        // blew up interpreting s\:- as a class name.
        String boom = "[\\s\\:-]";
        Compiler.compile(boom,  EnumSet.of(PatternFlags.ADVANCED));
    }

    @Test
    public void testConfusedQuantifier() throws Exception {
        String boom = "(?:(?:(?:[A-Z][a-zA-Z0-9.]*\\s)*[A-Z][a-zA-Z0-9.]*)(?=\\s?\\((?:ADX|AFET|AMEX|ASBA|ASE|BBV|BELEX|BEX|BIST|BLSE|BMV|BSE|BSSE|BSX|BVC|BVG|BVL|BVPA|BVQ|BVRJ|BdL|BgSE|BhSE|CBOE|CHX|CME|CNSX|CSE|CSX|DCSX|DFM|DGCX|DSE|ECSE|ESX|GSX|HKEx|HKMEx|HNX|HSE|HSX|IDX|IFB|IME|IOB|ISE|ISX|JSE|KASE|KFX|KLSE|KSE|LJSE|LSE|LSX|MAI|MCX|MERVAL|MNSE|MSE|MSEC|MSM|NASDAQ|NCDEX|NEPSE|NMCE|NSE|NSX|NYSE|OTCEI|PDEx|PHLX|PSE|PSEi|SASE|SEBI|SEC|SET|SGX|SICOM|SME|SMX|SSE|TASE|TASI|TFEX|TSE|TSXV|UPSE|USE|VMF|VSE|ZSE|orc)\\:[A-Z]{2,4}\\)))";
        RePattern pattern = HsrePattern.compile(boom, EnumSet.of(PatternFlags.ADVANCED));
        pattern.matcher("foo");
    }

    @Test
    public void testStockSymbol() throws Exception {
        String exp = "(?:(?:(?:[A-Z][a-zA-Z0-9.]*\\s)*[A-Z][a-zA-Z0-9.]*)(?=\\s?\\(\\s?[A-Z]{2,4}\\s?:\\s?[A-Z]{2,4}\\s?\\)))";
        RePattern pattern = HsrePattern.compile(exp, EnumSet.of(PatternFlags.ADVANCED));
        ReMatcher matcher = pattern.matcher("noise  - Titlecase Organization Name ( NYSE : TON ) - some noise");
        assertTrue(matcher.find());
        assertEquals(9, matcher.start());
        assertEquals(36, matcher.end());
        System.out.printf("%d %d\n", matcher.start(), matcher.end());

    }

    @Test
    public void testQuantifiedSubexpression() throws Exception {
        String exp = "([^\\s()<>]+|(\\([^\\s()<>]+\\)))*";
        Compiler.compile(exp, EnumSet.of(PatternFlags.ADVANCED));
    }

    @Test
    public void cannotFindOneCentimeter() throws Exception {
        // this is boiled down from the original.
        // not too bad for minimalization
        String exp = "(?i)\\m(?:(?:[\\+\\-]?\\d{1,3}(?:,?\\d{3})*(?:\\.\\d+)?))\\s?(?:(?:centi)?meters?)\\M";
        RePattern pattern = HsrePattern.compile(exp, EnumSet.of(PatternFlags.ADVANCED));
        ReMatcher matcher = pattern.matcher("this is 1 centimeter wide");
        assertTrue(matcher.find());
    }

    @Test
    public void cannotFindSimpleDate() throws Exception {
        //HsrePattern{pattern=\m(?:0?[1-9]|1[0-2])([-/\.\s])(?:0?[1-9]|[12]\d|3[01])\1(?:\d{2}|\d{4})\M}
        String exp = "\\m(?:0?[1-9]|1[0-2])([-/\\.\\s])(?:0?[1-9]|[12]\\d|3[01])\\1(?:\\d{2}|\\d{4})\\M";
        RePattern pattern = HsrePattern.compile(exp, EnumSet.of(PatternFlags.ADVANCED));
        ReMatcher matcher = pattern.matcher(")");
        matcher.reset(new InterruptibleCharSequence("1/1/1996".toCharArray(), 0, "1/1/1996".length()));
        matcher.region(0, 8);
        assertTrue(matcher.lookingAt());
    }

    @Test
    public void cesCannotFindDate() throws Exception {
        String exp = "\\m(?:\\d{1,2})[-/\\.\\s]+(?:\\d{1,2})[-/\\.\\s]+(?:\\d{1,4})\\.?\\M";
        String date = "02-08-2008";
        RePattern pattern = HsrePattern.compile(exp, EnumSet.of(PatternFlags.ADVANCED));
        ReMatcher matcher = pattern.matcher(")");
        matcher.reset(new InterruptibleCharSequence(date.toCharArray(), 0, date.length()));
        matcher.region(0, date.length());
        assertTrue(matcher.lookingAt());

    }
}
