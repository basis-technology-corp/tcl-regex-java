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

/**
 * Matcher. This is an incomplete analog of {@link java.util.regex.Matcher}.
 */
final class HsreMatcher implements ReMatcher {
    private CharSequence data;
    private final EnumSet<ExecFlags> flags;
    private final HsrePattern pattern;
    private Runtime runtime;
    private int regionStart;
    private int regionEnd;

    HsreMatcher(HsrePattern pattern, CharSequence data, EnumSet<ExecFlags> flags) {
        this.pattern = pattern;
        this.data = data;
        this.flags = flags;
        regionStart = 0;
        regionEnd = data.length();
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
    public boolean find(int startOffset) throws RegexException {
        if (startOffset < regionStart) {
            throw new IllegalArgumentException("Start offset less than region start");
        }
        // TODO: this is a pessimization; we should be able to make one at construction and reuse it.
        runtime = new Runtime();
        return runtime.exec(pattern, data.subSequence(startOffset, regionEnd), flags);
    }

    /**
     * Look for a match; begin the search at the start.
     * @return true for a match.
     */
    @Override
    public boolean find() throws RegexException {
        return find(regionStart);
    }

    @Override
    public ReMatcher region(int start, int end) throws RegexException {
        regionStart = start;
        regionEnd = end;
        return this;
    }

    @Override
    public ReMatcher reset() throws RegexException {
        regionStart = 0;
        regionEnd = data.length();
        return this;
    }

    @Override
    public ReMatcher reset(CharSequence newSequence) throws RegexException {
        data = newSequence;
        regionStart = 0;
        regionEnd = data.length();
        return this;
    }

    @Override
    public boolean matches() throws RegexException {
        if (!find()) {
            return false;
        }
        return start() == regionStart && end() == regionEnd;
    }

    @Override
    public int start() {
        return runtime.match.get(0).start + regionStart;
    }

    @Override
    public int start(int group) {
        return runtime.match.get(group).start + regionStart;
    }

    @Override
    public int end() {
        return runtime.match.get(0).end + regionStart;
    }

    @Override
    public int end(int group) {
        return runtime.match.get(group).end + regionStart;
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
