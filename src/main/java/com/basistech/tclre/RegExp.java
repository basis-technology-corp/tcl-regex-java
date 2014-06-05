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
class RegExp {
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

    static final int REG_NOTBOL =  0001;    /* BOS is not BOL */
    static final int REG_NOTEOL =  0002;    /* EOS is not EOL */
    long info;
    int nsub;       /* number of subexpressions */
    int csize;      /* sizeof(character) */
    Guts guts;
}
