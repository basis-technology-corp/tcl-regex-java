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

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


/**
 * A singelton class that maps non anchored patterns to their anchored counterpart.
 */
public enum AnchoredPatternsCache {
    INSTANCE;

    // avoiding (?: which is a group, not an option
    // Using a regex to parse a regex, clean brains off walls.
    private static final Pattern OPTION_PATTERN = Pattern.compile("\\(\\?[bceimnpqstwx]");

    private ConcurrentHashMap<HsrePattern, HsrePattern> anchordPatternCache =
            new ConcurrentHashMap<HsrePattern, HsrePattern>();

    /**
     * @param pattern an unachored pattern (does not start with ^)
     * @return the anchored counterpart of the input pattern.
     * @throws RegexException
     */
    private HsrePattern createAnchoredPattern(HsrePattern pattern) throws RegexException {
        //TODO: this will fail if the flags have something nasty like QUOTE.
        String originalPattern = pattern.pattern();
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

        return (HsrePattern)HsrePattern.compile(anchored, pattern.flags());
    }

    /**
     * Returns the anchored counterpart of the input pattern (uses caching).
     *
     * @param pattern an unachored pattern (does not start with ^)
     * @return the anchored counterpart of the input pattern.
     * @throws RegexException
     */
    public HsrePattern getAnchoredPattern(HsrePattern pattern) throws RegexException {
        if (!anchordPatternCache.containsKey(pattern)) {
            anchordPatternCache.putIfAbsent(pattern, createAnchoredPattern(pattern));
        }
        return anchordPatternCache.get(pattern);
    }

}