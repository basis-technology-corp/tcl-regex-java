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

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

/**
 * Created by benson on 6/11/14.
 */
public class Utils extends Assert {
    public static class MatcherMatches extends TypeSafeMatcher<ReMatcher> {
        final int start;
        final int end;
        final int index;

        public MatcherMatches(int index, int start, int end) {
            this.index = index;
            this.start = start;
            this.end = end;
        }

        @Override
        protected boolean matchesSafely(ReMatcher item) {
            return start == item.start(index) && end == item.end(index);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("Group %d was not [%d,%d)", index, start, end));
        }

        @Factory
        public static <T> Matcher<ReMatcher> groupIs(int index, int start, int end) {
            return new MatcherMatches(index, start, end);
        }
    }

    public static class Matches extends TypeSafeMatcher<String> {
        final RePattern pattern;
        final EnumSet<ExecFlags> eflags;

        Matches(String patternString, EnumSet<PatternFlags> pflags, EnumSet<ExecFlags> eflags) {
            try {
                pattern = HsrePattern.compile(patternString, pflags);
            } catch (RegexException e) {
                throw new RuntimeException(e);
            }
            this.eflags = eflags;

        }

        Matches(RePattern pattern, EnumSet<ExecFlags> eflags) {
            this.pattern = pattern;
            this.eflags = eflags;
        }

        @Override
        public boolean matchesSafely(String input) {
            ReMatcher matcher = pattern.matcher(input, eflags);
            try {
                return matcher.find();
            } catch (RegexException e) {
                throw new RuntimeException(e);
            }
        }

        public void describeTo(Description description) {
            description.appendText("matches " + pattern);
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern, EnumSet<PatternFlags> pflags, EnumSet<ExecFlags> eflags) {
            return new Matches(pattern, pflags, eflags);
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern) {
            return new Matches(pattern, EnumSet.noneOf(PatternFlags.class), EnumSet.noneOf(ExecFlags.class));
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern, PatternFlags... pflags) {
            EnumSet<PatternFlags> flagSet = EnumSet.noneOf(PatternFlags.class);
            for (PatternFlags pf : pflags) {
                flagSet.add(pf);
            }
            return new Matches(pattern, flagSet, EnumSet.noneOf(ExecFlags.class));
        }

        @Factory
        public static <T> Matcher<String> matches(RePattern pattern) {
            return new Matches(pattern, EnumSet.noneOf(ExecFlags.class));
        }

        @Factory
        public static <T> Matcher<String> matches(RePattern pattern, ExecFlags ... eflags) {
            EnumSet<ExecFlags> flagSet = EnumSet.noneOf(ExecFlags.class);
            for (ExecFlags ef : eflags) {
                flagSet.add(ef);
            }
            return new Matches(pattern, flagSet);
        }

    }
}
