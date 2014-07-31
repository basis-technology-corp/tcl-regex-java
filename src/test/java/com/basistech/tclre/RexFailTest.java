package com.basistech.tclre;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;

import java.io.*;
import java.net.URL;

/**
 * Created by bsg on 7/30/14.
 */
public class RexFailTest extends Utils {

    private String UTF16FileAsString(String resourceName) throws Exception {
        return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_16LE);
    }

    /** This test runs a certain regexp from the REX Japanese toolset against a certain buffer of REX test data
     * from the "Japanese 10MB" which caused a certain failure, namely a "get" of element 1 in a 0 index match array
     * in regexp runtime.java. The solution was that the "cfind" loop should clear the array by nulling its elements,
     * not by resizing it (the C code does the former).  This test forfends regression to that bug; if the bug is present,
     * it blows up.
     *
     * @throws Exception
     */

    @Test
    public void testRexFail() throws Exception {
        String data = UTF16FileAsString("bufferFrom2956.UTF-16LE.txt");
        String pattern = UTF16FileAsString("pattern.raw.utf-16LE.txt");
        RePattern pat = HsrePattern.compile(pattern, PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        ReMatcher match = pat.matcher(data);
        match.lookingAt();
    }
}
