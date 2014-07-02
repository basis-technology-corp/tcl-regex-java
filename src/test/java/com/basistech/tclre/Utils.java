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
 * Some handy test utilities.
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
        final String[] captures;

        Matches(String patternString, String[] captures, EnumSet<PatternFlags> pflags, EnumSet<ExecFlags> eflags) {
            try {
                pattern = HsrePattern.compile(patternString, pflags);
            } catch (RegexException e) {
                throw new RuntimeException(e);
            }
            this.eflags = eflags;
            this.captures = captures;

        }

        Matches(RePattern pattern, EnumSet<ExecFlags> eflags) {
            this.pattern = pattern;
            this.eflags = eflags;
            this.captures = null;
        }

        Matches(RePattern pattern, String[] captures, EnumSet<ExecFlags> eflags) {
            this.pattern = pattern;
            this.captures = captures;
            this.eflags = eflags;
        }

        @Override
        public boolean matchesSafely(String input) {
            ReMatcher matcher = pattern.matcher(input, eflags);
            if (!matcher.find()) {
                return false; //<soap>no</soap>
            }
            int nGroups = (captures == null) ? 0 : captures.length;
            if (matcher.groupCount() != nGroups) {
                return false;
            }
            for (int i = 0; i < nGroups; i++) {
                String mgroup = matcher.group(i + 1);
                if (!(mgroup.equals(captures[i]))) {
                    return false;
                }
            }
            return true;
        }

        public void describeTo(Description description) {

            description.appendText("matches " + pattern);
            if (captures != null) {
                description.appendText(", groups=[");
                for (String s : captures) {
                    description.appendText(" " + '"' + s + '"');
                }
                description.appendText("]");
            }
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern, EnumSet<PatternFlags> pflags, EnumSet<ExecFlags> eflags) {
            return new Matches(pattern, null, pflags, eflags);
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern) {
            return new Matches(pattern, null, EnumSet.noneOf(PatternFlags.class), EnumSet.noneOf(ExecFlags.class));
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern, PatternFlags... pflags) {
            EnumSet<PatternFlags> flagSet = EnumSet.noneOf(PatternFlags.class);
            for (PatternFlags pf : pflags) {
                flagSet.add(pf);
            }
            return new Matches(pattern, null, flagSet, EnumSet.noneOf(ExecFlags.class));
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

        @Factory
        public static <T> Matcher<String> matches(RePattern pattern, String[] captures, ExecFlags ... eflags) {
            EnumSet<ExecFlags> flagSet = EnumSet.noneOf(ExecFlags.class);
            for (ExecFlags ef : eflags) {
                flagSet.add(ef);
            }
            return new Matches(pattern, captures, flagSet);
        }

        @Factory
        public static <T> Matcher<String> matches(String pattern, String[] captures, PatternFlags... pflags) {
            EnumSet<PatternFlags> flagSet = EnumSet.noneOf(PatternFlags.class);
            for (PatternFlags pf : pflags) {
                flagSet.add(pf);
            }
            return new Matches(pattern, captures, flagSet, EnumSet.noneOf(ExecFlags.class));
        }
    }
}
