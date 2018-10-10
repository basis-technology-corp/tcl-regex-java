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

import com.google.common.base.MoreObjects;

import java.util.Collections;
import java.util.EnumSet;

/*
 * Matcher. This is an incomplete analog of {@link java.util.regex.Matcher}.
 */
final class HsreMatcher implements ReMatcher {

    private CharSequence data;
    private final EnumSet<ExecFlags> flags;
    private final HsrePattern pattern;
    private final Runtime runtime;
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
        runtime = new Runtime();
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
        return findInternal(pattern, startOffset, false);
    }

    private boolean findInternal(HsrePattern pat, int startOffset, boolean lookingAt) {
        if (startOffset < regionStart) {
            throw new IllegalArgumentException("Start offset less than region start");
        }

        // if lookingAt add the LOOKING_AT flag.
        EnumSet<ExecFlags> execFlags = flags;
        if (lookingAt) {
            execFlags = EnumSet.copyOf(flags);
            execFlags.add(ExecFlags.LOOKING_AT);
        }

        try {
            boolean found = runtime.exec(pat, data.subSequence(startOffset, regionEnd), execFlags);
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
        // if there are any matches sitting in the runtime, eliminate.
        if (runtime != null && runtime.match != null) {
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
        return findInternal(pattern, regionStart, true)
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
        return findInternal(pattern, regionStart, true);
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
        return MoreObjects.toStringHelper(this)
                .add("pattern", pattern)
                .add("flags", flags)
                .add("regionStart", regionStart)
                .add("regionEnd", regionEnd)
                .add("data", data)
                .toString();
    }
}
