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

import java.util.EnumSet;
import java.util.regex.MatchResult;

/**
 * A matcher; an object that uses a regular expression to search or match.
 * A has a {@link java.lang.CharSequence} that it applies to; it is created
 * over one, and can be switched to a new one with {@link #reset(CharSequence)}.
 * Matchers perform two operations: matching and searching. Matching ({@link #matches()}
 * attempts to satisfy the pattern with the entire string. Searching scans forward
 * through the string looking for a substring that satisfies the pattern.
 * <br/>
 * A matcher has a region. Initially, the region is the entire data, but
 * the application may call {@link #region(int, int)} to change it. Setting a region
 * limits the data examined by the matcher, but does not change the interpretation
 * of the offset passed to {@link #find(int)} or the values returned by the
 * methods of {@link java.util.regex.MatchResult}. These are always relative to the complete
 * data.
 * <br/>
 * Matchers support an iterative scanning process for multiple matches. After a successful call
 * to {@link #find()} or {@link #find(int)}, the matcher retains the offset of the end of the match.
 * A subsequent call to {@link #find()} starts at the character after the previous match.
 */
public interface ReMatcher extends MatchResult {
    /**
     * @return the pattern that produced this matcher.
     */
    RePattern pattern();

    /**
     * Search for the pattern in the data, starting at the specified offset.
     * If this returns true, then the methods from {@link java.util.regex.MatchResult}
     * return information about the match.
     * @param startOffset an offset relative to the beginning of the data.
     * @return true if the pattern was found
     */
    boolean find(int startOffset);

    /**
     * Search for the pattern in the data, starting at the end of the previous match.
     * If there was no previous match, starts at the beginning of the region.
     * @return if the pattern was found
     */
    boolean find();

    /**
     * Specify a region that bounds searching and matching. The region is
     * [start, end]. This resets the current search position to the start
     * of the region.
     * @param start the start offset, relative to the entire data.
     * @param end the end offset, relative to the entire data.
     * @return this matcher.
     */
    ReMatcher region(int start, int end);

    /**
     * Reset this matcher to the state as of the creation or the last
     * call to {@link #reset(CharSequence)}.
     * @return this matcher
     */
    ReMatcher reset();

    /**
     * Change the data for this matcher. The region is set to the entire
     * sequence, and the previous match information is reset.
     * @param newSequence the new data.
     * @return this matcher
     */
    ReMatcher reset(CharSequence newSequence);

    /**
     * Change the flags associated with this matcher.
     * @param flags the new flags.
     * @return this matcher.
     */
    ReMatcher flags(ExecFlags ... flags);

    /**
     * @return return the current set of flags associated with this matcher.
     */
    EnumSet<ExecFlags> flags();

    /**
     * Perform a match operation.
     * @return true if the pattern is satisfied by the entire region.
     */
    boolean matches();

    /**
     * @return the current region start.
     */
    int regionStart();

    /**
     * @return the current region end.
     */
    int regionEnd();

    /**
     * attempt to satisfy the pattern with the characters starting at the current position.
     * @return true if the pattern is satisfied.
     */
    boolean lookingAt();
}
