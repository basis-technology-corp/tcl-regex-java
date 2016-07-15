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

/**
 * A compiled regular expression.
 */
public interface RePattern {
    /**
     * Create a matcher from a pattern.
     * @param data the initial data that the matcher will process; {@link com.basistech.tclre.ReMatcher#reset(CharSequence)}
     *             may be used to change it.
     * @param flags optional flags that change the behavior of the matcher.
     * @return the matcher
     */
    ReMatcher matcher(CharSequence data, ExecFlags... flags);
    /**
     * Create a matcher from a pattern.
     * @param data the initial data that the matcher will process; {@link com.basistech.tclre.ReMatcher#reset(CharSequence)}
     *             may be used to change it.
     * @param flags optional flags that change the behavior of the matcher.
     * @return the matcher
     */
    ReMatcher matcher(CharSequence data, EnumSet<ExecFlags> flags);

    /**
     * @return string representation of pattern.
     */
    String pattern();

    /**
     * @return return flags.
     */
    EnumSet<PatternFlags> flags();
}
