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
}
