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

import java.util.Collections;
import java.util.EnumSet;
import java.util.regex.Pattern;

import com.google.common.base.Objects;

/*
 * Matcher. This is an incomplete analog of {@link java.util.regex.Matcher}.
 */
final class HsreMatcher implements ReMatcher {
    // avoiding (?: which is a group, not an option
    // Using a regex to parse a regex, clean brains off walls.
    private static final Pattern OPTION_PATTERN = Pattern.compile("\\(\\?[bceimnpqstwx]");
    private CharSequence data;
    private final EnumSet<ExecFlags> flags;
    private final HsrePattern pattern;
    /*
     * We need an extra pattern with ^ at the front to do an efficient job on
     * lookingAt and matches. No need for a third with $ for matches; that would
     * not save work.
     */
    private final HsrePattern startAnchoredPattern;
    private Runtime runtime;
    private int regionStart;
    private int regionEnd;
    private int nextFindOffset;
    // correction from Runtime.matches to us.
    private int matchOffset;

    HsreMatcher(HsrePattern pattern, CharSequence data, EnumSet<ExecFlags> flags) throws RegexException {
        this.pattern = pattern;
        this.data = data;
        this.flags = flags;
        regionStart = 0;
        regionEnd = data.length();

        String originalPattern = pattern.getOriginal();
        if (originalPattern.length() > 0 && originalPattern.charAt(0) == '^') {
            this.startAnchoredPattern = this.pattern;
        } else {
            //TODO: this will fail if the flags have something nasty like QUOTE.

            String anchored;
            if (OPTION_PATTERN.matcher(originalPattern).lookingAt()) {
                // (?...) for defined options.
                int endOpEx = originalPattern.indexOf(')');
                anchored = originalPattern.substring(0, endOpEx + 1) + "^" + originalPattern.substring(endOpEx + 1);
            } else if (originalPattern.startsWith("***=")) {
                throw new RegexException("Patterns with the ***= director are not supported");
            } else if (originalPattern.startsWith("***:")) {
                anchored = "***:^" + originalPattern.substring(4);
            } else if (originalPattern.startsWith("***")) {
                //TODO: move to pattern compilation.
                throw new RegexException("Invalid *** director");
            } else {
                anchored = "^" + originalPattern;
            }

            this.startAnchoredPattern = (HsrePattern)HsrePattern.compile(anchored, pattern.getOriginalFlags());
        }

    }

    /**
     * @return the pattern.
     */
    @Override
    public RePattern pattern() {
        return pattern;
    }

    /**
     * Look for a match; begin the search at a given offset.
     * @param startOffset the offset.
     * @return true for a match.
     */
    @Override
    public boolean find(int startOffset) throws RegexRuntimeException {
        return findInternal(pattern, startOffset);
    }

    private boolean findInternal(HsrePattern pat, int startOffset) {
        if (startOffset < regionStart) {
            throw new IllegalArgumentException("Start offset less than region start");
        }
        // TODO: this is a pessimization; we should be able to make one at construction and reuse it.
        runtime = new Runtime();
        try {
            boolean found = runtime.exec(pat, data.subSequence(startOffset, regionEnd), flags);
            if (found) {
                // note how much to add to the runtime.match offsets.
                matchOffset = startOffset;
                // and now end does the necessary correction on the offset value from 'runtime'.
                nextFindOffset = end();
            }
            return found;
        } catch (RegexException e) {
            throw new RegexRuntimeException(e);
        }
    }

    /**
     * Look for a match; begin the search at the start.
     * @return true for a match.
     */
    @Override
    public boolean find() throws RegexRuntimeException {
        return find(nextFindOffset);
    }

    /*
     * Called by all the 'resetting' functions.
     * Allows find to work as specified.
    */
    private void resetState() {
        if (runtime != null) {
            runtime.match.clear();
        }
        nextFindOffset = regionStart;
    }

    @Override
    public ReMatcher region(int start, int end) throws RegexRuntimeException {
        regionStart = start;
        regionEnd = end;
        resetState();
        return this;
    }

    @Override
    public ReMatcher reset() throws RegexRuntimeException {
        regionStart = 0;
        regionEnd = data.length();
        resetState();
        return this;
    }

    @Override
    public ReMatcher reset(CharSequence newSequence) throws RegexRuntimeException {
        data = newSequence;
        regionStart = 0;
        regionEnd = data.length();
        resetState();
        return this;
    }

    @Override
    public ReMatcher flags(ExecFlags... flags) {
        this.flags().clear();
        Collections.addAll(this.flags, flags);
        return null;
    }

    @Override
    public EnumSet<ExecFlags> flags() {
        return flags;
    }


    @Override
    public boolean matches() throws RegexRuntimeException {
        return findInternal(startAnchoredPattern, regionStart)
                && start() == regionStart
                && end() == regionEnd;
    }

    @Override
    public int regionStart() {
        return regionStart;
    }

    @Override
    public int regionEnd() {
        return regionEnd;
    }

    @Override
    public boolean lookingAt() {
        return findInternal(startAnchoredPattern, regionStart);
    }

    @Override
    public int start() {
        return runtime.match.get(0).start + matchOffset;
    }

    @Override
    public int start(int group) {
        return runtime.match.get(group).start + matchOffset;
    }

    @Override
    public int end() {
        return runtime.match.get(0).end + matchOffset;
    }

    @Override
    public int end(int group) {
        return runtime.match.get(group).end + matchOffset;
    }

    @Override
    public String group() {
        return data.subSequence(start(), end()).toString();
    }

    @Override
    public String group(int group) {
        return data.subSequence(start(group), end(group)).toString();
    }

    @Override
    public int groupCount() {
        return runtime.match.size() - 1; // omit the 'group' for the whole match.
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("pattern", pattern)
                .add("flags", flags)
                .add("regionStart", regionStart)
                .add("regionEnd", regionEnd)
                .add("data", data)
                .toString();
    }
}
