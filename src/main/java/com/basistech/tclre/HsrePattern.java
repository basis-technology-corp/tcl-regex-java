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

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumSet;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * A compiled regular expression. The method {@link #compile(String, PatternFlags...)} serves
 * as the factory.
 * @see com.basistech.tclre.RePattern
 * @see com.basistech.tclre.ReMatcher
 */
public class HsrePattern implements RePattern, Serializable {
    static final long serialVersionUID = 1L;
    final long info;
    final int nsub;       /* number of subexpressions */
    final Guts guts;
    final String original;
    final EnumSet<PatternFlags> originalFlags;

    HsrePattern(String original, EnumSet<PatternFlags> originalFlags, long info, int nsub, Guts guts) {
        this.original = original;
        this.originalFlags = originalFlags;
        this.info = info;
        this.nsub = nsub;
        this.guts = guts;
    }

    /**
     * Compile a pattern.
     * @param pattern the pattern.
     * @param flags flags that determine the interpretation of the pattern.
     * @return the compiled pattern.
     * @throws RegexException
     */
    public static RePattern compile(String pattern, EnumSet<PatternFlags> flags) throws RegexException {
        return Compiler.compile(pattern, flags);
    }

    /**
     * Compile a pattern.
     * @param pattern the pattern.
     * @param flags flags that determine the interpretation of the pattern.
     * @return the compiled pattern.
     * @throws RegexException
     */
    public static RePattern compile(String pattern, PatternFlags... flags) throws RegexException {
        EnumSet<PatternFlags> flagSet = EnumSet.noneOf(PatternFlags.class);
        Collections.addAll(flagSet, flags);
        return Compiler.compile(pattern, flagSet);
    }

    @Override
    public HsreMatcher matcher(CharSequence data, ExecFlags... flags) {
        EnumSet<ExecFlags> flagSet = EnumSet.noneOf(ExecFlags.class);
        Collections.addAll(flagSet, flags);
        try {
            return new HsreMatcher(this, data, flagSet);
        } catch (RegexException e) {
            // the idea is that we've done all the error detection before.
            throw new RegexRuntimeException(e);
        }

    }

    @Override
    public HsreMatcher matcher(CharSequence data, EnumSet<ExecFlags> flags) {
        try {
            return new HsreMatcher(this, data, flags);
        } catch (RegexException e) {
            throw new RegexRuntimeException(e);
        }
    }

    @Override
    public String pattern() {
        return original;
    }

    @Override
    public EnumSet<PatternFlags> flags() {
        return originalFlags;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pattern", original)
                .add("flags", originalFlags)
                .toString();
    }
}
