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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

/**
 * Grep command line to exercise the regex package.
 */
public final class Grep {
    @Argument(required = true)
    String pattern;
    @Argument(index = 1)
    List<File> inputs;

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
        // For now, internal hack to experiment with.
        RegExp regexp = new RegExp(); // dummy
        Compiler compiler = new Compiler(regexp, pattern, 0);
        compiler.compile();

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
