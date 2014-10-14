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