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
* Nfa representation.
*/
class Nfa {
    State pre;	/* pre-initial state */
    State init;	/* initial state */
    State finalState;	/* final state */
    State post;	/* post-final state */
    int nstates;		/* for numbering states */
    State states;	/* state-chain header */
    State slast;	/* tail of the chain */
    State free;	/* free list */
    ColorMap cm;	/* the color map */
    short[] bos = new short[2];		/* colors, if any, assigned to BOS and BOL */
    short[] eos = new short[2];		/* colors, if any, assigned to EOS and EOL */
    Compiler v;		/* simplifies compile error reporting */
    Nfa parent;	/* parent NFA, if any */
}
