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
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * Grep command line to exercise the regex package.
 */
@SuppressWarnings("PMD")
public final class Grep {
    @Argument(required = true)
    String pattern;
    @Argument(index = 1)
    List<File> inputs;

    private RePattern re;

    private Grep() {
        //
    }

    public static void main(String[] args) throws IOException, RegexException {
        Grep that = new Grep();
        CmdLineParser parser = new CmdLineParser(that);
        try {
            if (args.length == 0) {
                System.err.println("grep PATTERN file1 ... fileN");
                parser.printUsage(System.err);
                return;
            }
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.exit(1);
        }

        that.go();

    }

    private void go() throws IOException, RegexException {
        re = Compiler.compile(pattern, EnumSet.of(PatternFlags.EXTENDED, PatternFlags.ADVANCED));
        for (File input : inputs) {
            processFile(input);
        }

    }

    private void processFile(File input) throws IOException, RegexException {
        CharSource inputCharSource = Files.asCharSource(input, Charsets.UTF_8);
        processReader(inputCharSource.openBufferedStream());
    }

    private void processReader(BufferedReader bufferedReader) throws IOException, RegexException {
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            processOneLine(line);
        }
    }

    private void processOneLine(String line) throws RegexException {
        // not trying to match yet.
    }
}
