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
import java.util.regex.MatchResult;

/**
 * Matcher. This is an incomplete analog of {@link java.util.regex.Matcher}.
 */
public final class HsreMatcher implements ReMatcher, MatchResult {
    private final CharSequence data;
    private final EnumSet<ExecFlags> flags;
    private final HsrePattern pattern;
    private Runtime runtime;

    HsreMatcher(HsrePattern pattern, CharSequence data, EnumSet<ExecFlags> flags) {
        this.pattern = pattern;
        this.data = data;
        this.flags = flags;
    }

    /**
     * @return the pattern.
     */
    @Override
    public HsrePattern pattern() {
        return pattern;
    }

    /**
     * Look for a match; begin the search at a given offset.
     * @param startOffset the offset.
     * @return true for a match.
     */
    @Override
    public boolean find(int startOffset) throws RegexException {
        // TODO: this is a pessimization; we should be able to make one at construction and reuse it.
        runtime = new Runtime();
        return runtime.exec(pattern, data.subSequence(startOffset, data.length()), flags);
    }

    /**
     * Look for a match; begin the search at the start.
     * @return true for a match.
     */
    @Override
    public boolean find() throws RegexException {
        return find(0);
    }

    @Override
    public int start() {
        return runtime.match.get(0).start;
    }

    @Override
    public int start(int group) {
        return runtime.match.get(group).start;
    }

    @Override
    public int end() {
        return runtime.match.get(0).start;
    }

    @Override
    public int end(int group) {
        return runtime.match.get(group).end;
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
}
