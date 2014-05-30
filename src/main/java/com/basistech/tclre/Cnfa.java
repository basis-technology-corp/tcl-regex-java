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

/**
* Created by benson on 5/29/14.
*/
class Cnfa {
    static final int HASLACONS = 1;
    int nstates;		/* number of states */
    int ncolors;		/* number of colors */
    int flags;
    int pre;		/* setup state number */
    int post;		/* teardown state number */
    short[] bos = new short[2];		/* colors, if any, assigned to BOS and BOL */
    short[] eos = new short[2];		/* colors, if any, assigned to EOS and EOL */
    Carc[][] states;	/* vector of pointers to outarc lists */
    Carc[] arcs;	/* the area for the lists */

    void zapcnfa() {
        nstates = 0;
    }

    boolean nullcnfa() {
        return nstates == 0;
    }

}
