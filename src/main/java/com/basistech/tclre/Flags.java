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

/**
 * A collection of integer flags.
 */
final class Flags {
    static final int REG_BASIC = 000000;    /* BREs (convenience) */
    static final int REG_EXTENDED = 000001; /* EREs */
    static final int REG_ADVF = 000002; /* advanced features in EREs */
    static final int REG_ADVANCED = 000003; /* AREs (which are also EREs) */
    static final int REG_QUOTE = 000004;    /* no special characters, none */
    static final int REG_NOSPEC = REG_QUOTE;    /* historical synonym */
    static final int REG_ICASE = 000010;    /* ignore case */
    static final int REG_NOSUB = 000020;    /* don't care about subexpressions */
    static final int REG_EXPANDED = 000040; /* expanded format, white space & comments */
    static final int REG_NLSTOP = 000100;   /* \n doesn't match . or [^ ] */
    static final int REG_NLANCH = 000200;   /* ^ matches after \n, $ before */
    static final int REG_NEWLINE = 000300;  /* newlines are line terminators */
    static final int REG_PEND = 000400; /* ugh -- backward-compatibility hack */
    static final int REG_LOOKING_AT = 001000;   /* act as if there was a ^ */
    static final int REG_BOSONLY = 002000;  /* temporary kludge for BOS-only matches */
    // Next two not used.
//    static final int REG_DUMP = 004000; /* none of your business :-) */
//    static final int REG_FAKE = 010000; /* none of your business :-) */
    static final int REG_PROGRESS = 020000; /* none of your business :-) */
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
    static final int REG_ULOCALE = 002000;
    static final int REG_UEMPTYMATCH = 004000;
    static final int REG_UIMPOSSIBLE = 010000;
    static final int REG_USHORTEST = 020000;

    static final int REG_NOTBOL =  1;    /* BOS is not BOL */
    static final int REG_NOTEOL =  2;    /* EOS is not EOL */

    private Flags() {
        //
    }


}
