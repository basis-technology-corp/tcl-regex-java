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
final class Flags {
    private Flags() {
        //
    }

    static final int REG_BASIC = 000000;	/* BREs (convenience) */
    static final int REG_EXTENDED = 000001;	/* EREs */
    static final int REG_ADVF = 000002;	/* advanced features in EREs */
    static final int REG_ADVANCED = 000003;	/* AREs (which are also EREs) */
    static final int REG_QUOTE = 000004;	/* no special characters, none */
    static final int REG_NOSPEC = REG_QUOTE;	/* historical synonym */
    static final int REG_ICASE = 000010;	/* ignore case */
    static final int REG_NOSUB = 000020;	/* don't care about subexpressions */
    static final int REG_EXPANDED = 000040;	/* expanded format, white space & comments */
    static final int REG_NLSTOP = 000100;	/* \n doesn't match . or [^ ] */
    static final int REG_NLANCH = 000200;	/* ^ matches after \n, $ before */
    static final int REG_NEWLINE = 000300;	/* newlines are line terminators */
    static final int REG_PEND = 000400;	/* ugh -- backward-compatibility hack */
    static final int REG_EXPECT = 001000;	/* report details on partial/limited matches */
    static final int REG_BOSONLY = 002000;	/* temporary kludge for BOS-only matches */
    static final int REG_DUMP = 004000;	/* none of your business :-) */
    static final int REG_FAKE = 010000;	/* none of your business :-) */
    static final int REG_PROGRESS = 020000;	/* none of your business :-) */
    static final int REG_NOCAPT = 040000; /* disable capturing parens */

    // these are internal. This may need sorting.
    static final int REG_UBACKREF = 000001;
    static final int REG_ULOOKAHEAD = 000002;
    static final int REG_UBOUNDS = 000004;
    static final int REG_UBRACES = 000010;
    static final int REG_UBSALNUM = 000020;
    static final int REG_UPBOTCH = 000040;
    static final int REG_UBBS = 000100;
    static final int REG_UNONPOSIX = 000200;
    static final int REG_UUNSPEC = 000400;
    static final int REG_UUNPORT = 001000;
    static final int REG_ULOCALE = 002000;
    static final int REG_UEMPTYMATCH = 004000;
    static final int REG_UIMPOSSIBLE = 010000;
    static final int REG_USHORTEST = 020000;


}
