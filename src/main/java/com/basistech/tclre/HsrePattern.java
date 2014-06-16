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

import com.google.common.base.Objects;

/**
 * A compiled regular expression. The method {@link #compile(String, PatternFlags...)} serves
 * as the factory.
 * @see com.basistech.tclre.RePattern
 * @see com.basistech.tclre.ReMatcher
 */
public class HsrePattern implements RePattern {
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

    String getOriginal() {
        return original;
    }

    EnumSet<PatternFlags> getOriginalFlags() {
        return originalFlags;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("pattern", original)
                .add("flags", originalFlags)
                .toString();
    }
}
