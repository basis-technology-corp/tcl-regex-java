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

import java.util.EnumSet;
import java.util.regex.Pattern;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

/**
 * Some handy test utilities.
 */
public class Utils {
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
            if (captures == null) {
                return true;
            }
            int nGroups = captures.length;
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
        public static <T> Matcher<String> matches(String pattern, String[] captures, EnumSet<PatternFlags> pflags, EnumSet<ExecFlags> eflags) {
            return new Matches(pattern, captures, pflags, eflags);
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
