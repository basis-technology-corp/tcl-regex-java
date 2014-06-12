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

import com.google.common.base.Objects;

/**
 * A compiled regular expression. The method {@link #compile(String, java.util.EnumSet)} serves
 * as the factory.
 */
public class HsrePattern implements RePattern {
    final long info;
    final int nsub;       /* number of subexpressions */
    final Guts guts;
    final String original;

    HsrePattern(String original, long info, int nsub, Guts guts) {
        this.original = original;
        this.info = info;
        this.nsub = nsub;
        this.guts = guts;
    }

    /**
     * Compile a pattern.
     * @param pattern the pattern.
     * @param flags flags that determine the interpretation of the pattern.
     * @return the pattern.
     * @throws RegexException
     */
    static RePattern compile(String pattern, EnumSet<PatternFlags> flags) throws RegexException {
        return Compiler.compile(pattern, flags);
    }

    static RePattern compile(String pattern, PatternFlags... flags) throws RegexException {
        EnumSet<PatternFlags> flagSet = EnumSet.noneOf(PatternFlags.class);
        for (PatternFlags f : flags) {
            flagSet.add(f);
        }
        return Compiler.compile(pattern, flagSet);
    }

    @Override
    public HsreMatcher matcher(CharSequence data, ExecFlags... flags) {
        EnumSet<ExecFlags> flagSet = EnumSet.noneOf(ExecFlags.class);
        for (ExecFlags f : flags) {
            flagSet.add(f);
        }
        return new HsreMatcher(this, data, flagSet);

    }

    @Override
    public HsreMatcher matcher(CharSequence data, EnumSet<ExecFlags> flags) {
        return new HsreMatcher(this, data, flags);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("pattern", original)
                .toString();
    }
}
