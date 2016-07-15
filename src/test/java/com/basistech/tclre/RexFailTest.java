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

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.junit.Test;


public class RexFailTest extends Utils {

    private String readFile(String resourceName) throws Exception {
        return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_16LE);
    }

    /** This test runs a certain regexp from the REX Japanese toolset against a certain buffer of REX test data
     * from the "Japanese 10MB" which caused a certain failure, namely a "get" of element 1 in a 0 index match array
     * in regexp runtime.java. The solution was that the "cfind" loop should clear the array by nulling its elements,
     * not by resizing it (the C code does the former).  This test forfends regression to that bug; if the bug is present,
     * it blows up.
     */

    @Test
    public void testRexFail() throws Exception {
        String data = readFile("bufferFrom2956.UTF-16LE.txt");
        String pattern = readFile("pattern.raw.utf-16LE.txt");
        RePattern pat = HsrePattern.compile(pattern, PatternFlags.ADVANCED, PatternFlags.EXPANDED);
        ReMatcher match = pat.matcher(data);
        match.lookingAt();
    }
}
