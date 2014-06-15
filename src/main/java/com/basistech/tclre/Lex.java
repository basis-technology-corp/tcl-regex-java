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
 * from regc_lex.c
 * Note that this continues the pattern of all the state living
 * in the 'Compiler' object.
 */
class Lex {
    /* lexical contexts */
    static final int L_ERE = 1; /* mainline ERE/ARE */
    static final int L_BRE = 2; /* mainline BRE */
    static final int L_Q = 3;   /* Flags.REG_QUOTE */
    static final int L_EBND = 4; /* ERE/ARE bound */
    static final int L_BBND = 5;    /* BRE bound */
    static final int L_BRACK = 6;   /* brackets */
    static final int L_CEL = 7; /* collating element */
    static final int L_ECL = 8; /* equivalence class */
    static final int L_CCL = 9; /* character class */

    /*
     * string constants to interpolate as expansions of things like \d
     */
    //CHECKSTYLE:OFF
    static final char backd[] = {       /* \d */
            '[', '[', ':',
            'd', 'i', 'g', 'i', 't',
            ':', ']', ']'
    };

    static final char backD[] = {       /* \D */
            '[', '^', '[', ':',
            'd', 'i', 'g', 'i', 't',
            ':', ']', ']'
    };
    static final char brbackd[] = { /* \d within brackets */
            '[', ':',
            'd', 'i', 'g', 'i', 't',
            ':', ']'
    };
    static final char backs[] = {       /* \s */
            '[', '[', ':',
            's', 'p', 'a', 'c', 'e',
            ':', ']', ']'
    };
    static final char backS[] = {       /* \S */
            '[', '^', '[', ':',
            's', 'p', 'a', 'c', 'e',
            ':', ']', ']'
    };
    static final char brbacks[] = { /* \s within brackets */
            '[', ':',
            's', 'p', 'a', 'c', 'e',
            ':', ']'
    };
    static final char backw[] = {       /* \w */
            '[', '[', ':',
            'a', 'l', 'n', 'u', 'm',
            ':', ']', '_', ']'
    };
    static final char backW[] = {       /* \W */
            '[', '^', '[', ':',
            'a', 'l', 'n', 'u', 'm',
            ':', ']', '_', ']'
    };
    static final char brbackw[] = { /* \w within brackets */
            '[', ':',
            'a', 'l', 'n', 'u', 'm',
            ':', ']', '_'
    };
    //CHECKSTYLE:ON

    private Compiler v;

    Lex(Compiler v) {
        this.v = v;
    }


    boolean see(int t) {
        return v.nexttype == t;
    }

    private char charAt(int index) {
        return v.pattern[index];
    }

    private char charAtNow() {
        return charAt(v.now);
    }

    private char charAtNowAdvance() {
        return charAt(v.now++);
    }

    private char charAtNowPlus(int offset) {
        return charAt(v.now + offset);
    }

    /* scanning macros (know about v) */
    boolean ateos() {
        return v.now >= v.stop;
    }

    boolean have(int n) {
        return v.stop - v.now >= n;
    }

    boolean next1(char c) {
        return !ateos() && charAtNow() == c;
    }

    boolean next2(char a, char b) {
        return have(2) && charAtNow() == a && charAtNowPlus(1) == b;
    }

    boolean next3(char a, char b, char c) {
        return have(3)
                && charAtNow() == a
                && charAtNowPlus(1) == b
                && charAtNowPlus(2) == c;
    }

    void set(int c) {
        v.nexttype = c;
    }

    void set(int c, int n) {
        v.nexttype = c;
        v.nextvalue = n;
    }

    boolean ret(int c) {
        set(c);
        return true;
    }

    boolean retv(int c, int n) {
        set(c, n);
        return true;
    }

    boolean lasttype(int t) {
        return v.lasttype == t;
    }

    void intocon(int c) {
        v.lexcon = c;
    }

    boolean incon(int c) {
        return v.lexcon == c;
    }

    /**
     * lexstart - set up lexical stuff, scan leading options
     */
    void lexstart() throws RegexException {
        prefixes();         /* may turn on new type bits etc. */

        if (0 != (v.cflags & Flags.REG_QUOTE)) {
            assert 0 == (v.cflags & (Flags.REG_ADVANCED | Flags.REG_EXPANDED | Flags.REG_NEWLINE));
            intocon(L_Q);
        } else if (0 != (v.cflags & Flags.REG_EXTENDED)) {
            assert 0 == (v.cflags & Flags.REG_QUOTE);
            intocon(L_ERE);
        } else {
            assert 0 == (v.cflags & (Flags.REG_QUOTE | Flags.REG_ADVF));
            intocon(L_BRE);
        }

        v.nexttype = Compiler.EMPTY;        /* remember we were at the start */
        next();         /* set up the first token */
    }

    boolean iscalpha(char c) {
        return c < 0x80 && Character.isLetter(c);
    }

    boolean iscdigit(char x) {
        return x < 0x80 && Character.isDigit(x);
    }

    boolean iscalnum(char x) {
        return x < 0x80 && Character.isLetterOrDigit(x);
    }

    boolean iscspace(char x) {
        return x < 0x80 && Character.isSpaceChar(x);
    }

    /**
     * prefixes - implement various special prefixes
     */
    void prefixes() throws RegexException {
    /* literal string doesn't get any of this stuff */
        if (0 != (v.cflags & Flags.REG_QUOTE)) {
            return;
        }

    /* initial "***" gets special things */
        if (have(4) && next3('*', '*', '*')) {
            switch (charAtNowPlus(3)) {
            case '?':       /* "***?" error, msg shows version */
                throw new RegexException("REG_BADPAT");
            case '=':       /* "***=" shifts to literal string */
                v.note(Flags.REG_UNONPOSIX);
                v.cflags |= Flags.REG_QUOTE;
                v.cflags &= ~(Flags.REG_ADVANCED | Flags.REG_EXPANDED | Flags.REG_NEWLINE);
                v.now += 4;
                return;     /* and there can be no more prefixes */
            case ':':       /* "***:" shifts to AREs */
                v.note(Flags.REG_UNONPOSIX);
                v.cflags |= Flags.REG_ADVANCED;
                v.now += 4;
                break;
            default:        /* otherwise *** is just an error */
                throw new RegexException("REG_BADRPT");
            }
        }

    /* BREs and EREs don't get embedded options */
        if ((v.cflags & Flags.REG_ADVANCED) != Flags.REG_ADVANCED) {
            return;
        }

    /* embedded options (AREs only) */
        if (have(3) && next2('(', '?') && iscalpha(charAtNowPlus(2))) {
            v.note(Flags.REG_UNONPOSIX);
            v.now += 2;
            for (; !ateos() && iscalpha(charAtNow()); v.now++) {
                switch (charAtNow()) {
                case 'b':       /* BREs (but why???) */
                    v.cflags &= ~(Flags.REG_ADVANCED | Flags.REG_QUOTE);
                    break;
                case 'c':       /* case sensitive */
                    v.cflags &= ~Flags.REG_ICASE;
                    break;
                case 'e':       /* plain EREs */
                    v.cflags |= Flags.REG_EXTENDED;
                    v.cflags &= ~(Flags.REG_ADVF | Flags.REG_QUOTE);
                    break;
                case 'i':       /* case insensitive */
                    v.cflags |= Flags.REG_ICASE;
                    break;
                case 'm':       /* Perloid synonym for n */
                case 'n':       /* \n affects ^ $ . [^ */
                    v.cflags |= Flags.REG_NEWLINE;
                    break;
                case 'p':       /* ~Perl, \n affects . [^ */
                    v.cflags |= Flags.REG_NLSTOP;
                    v.cflags &= ~Flags.REG_NLANCH;
                    break;
                case 'q':       /* literal string */
                    v.cflags |= Flags.REG_QUOTE;
                    v.cflags &= ~Flags.REG_ADVANCED;
                    break;
                case 's':       /* single line, \n ordinary */
                    v.cflags &= ~Flags.REG_NEWLINE;
                    break;
                case 't':       /* tight syntax */
                    v.cflags &= ~Flags.REG_EXPANDED;
                    break;
                case 'w':       /* weird, \n affects ^ $ only */
                    v.cflags &= ~Flags.REG_NLSTOP;
                    v.cflags |= Flags.REG_NLANCH;
                    break;
                case 'x':       /* expanded syntax */
                    v.cflags |= Flags.REG_EXPANDED;
                    break;
                default:
                    throw new RegexException("REG_BADOPT");
                }
            }

            if (!next1(')')) {
                throw new RegexException("REG_BADOPT");
            }
            v.now++;
            if (0 != (v.cflags & Flags.REG_QUOTE)) {
                v.cflags &= ~(Flags.REG_EXPANDED | Flags.REG_NEWLINE);
            }
        }
    }

    /**
     * lexnest - "call a subroutine", interpolating string at the lexical level
     * Note, this is not a very general facility.  There are a number of
     * implicit assumptions about what sorts of strings can be subroutines.
     */
    void lexnest(char[] interpolated) {
        assert v.savepattern == null;   /* only one level of nesting */
        v.savepattern = v.pattern;
        v.savenow = v.now;
        v.savestop = v.stop;
        v.savenow = v.now;
        v.pattern = interpolated;
        v.now = 0;
        v.stop = v.pattern.length;
    }

    /**
     * lexword - interpolate a bracket expression for word characters
     * Possibly ought to inquire whether there is a "word" character class.
     */
    void lexword() {
        lexnest(backw);
    }

    int digitval(char c) {
        return c - '0';
    }

    void note(long n) {
        v.note(n);
    }

    //CHECKSTYLE:OFF
    /**
     * next - get next token
     */
    boolean next() throws RegexException {
        char c;

    /* remember flavor of last token */
        v.lasttype = v.nexttype;

    /* REG_BOSONLY */
        if (v.nexttype == Compiler.EMPTY && (0 != (v.cflags & Flags.REG_BOSONLY))) {
        /* at start of a REG_BOSONLY RE */
            return retv(Compiler.SBEGIN, (char)0);      /* same as \A */
        }

    /* if we're nested and we've hit end, return to outer level */
        if (v.savepattern != null && ateos()) {
            v.now = v.savenow;
            v.stop = v.savestop;
            v.savenow = -1;
            v.savestop = -1;
            v.pattern = v.savepattern;
            v.savepattern = null; // mark that it's not saved.
        }

    /* skip white space etc. if appropriate (not in literal or []) */
        if (0 != (v.cflags & Flags.REG_EXPANDED)) {
            switch (v.lexcon) {
            case L_ERE:
            case L_BRE:
            case L_EBND:
            case L_BBND:
                skip();
                break;
            }
        }

    /* handle EOS, depending on context */
        if (ateos()) {
            switch (v.lexcon) {
            case L_ERE:
            case L_BRE:
            case L_Q:
                return ret(Compiler.EOS);
            case L_EBND:
            case L_BBND:
                throw new RegexException("Unbalanced braces.");
            case L_BRACK:
            case L_CEL:
            case L_ECL:
            case L_CCL:
                throw new RegexException("Unbalanced brackets.");
            }
            assert false;
        }

    /* okay, time to actually get a character */
        c = charAtNowAdvance();

    /* deal with the easy contexts, punt EREs to code below */
        switch (v.lexcon) {
        case L_BRE:         /* punt BREs to separate function */
            return brenext(c);
        case L_ERE:         /* see below */
            break;
        case L_Q:           /* literal strings are easy */
            return retv(Compiler.PLAIN, c);
        case L_BBND:            /* bounds are fairly simple */
        case L_EBND:
            switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return retv(Compiler.DIGIT, digitval(c));
            case ',':
                return ret(',');
            case '}':       /* ERE bound ends with } */
                if (incon(L_EBND)) {
                    intocon(L_ERE);
                    if (0 != (v.cflags & Flags.REG_ADVF) && next1('?')) {
                        v.now++;
                        note(Flags.REG_UNONPOSIX);
                        return retv('}', 0);
                    }
                    return retv('}', 1);
                } else {
                    throw new RegexException("Errors.REG_BADBR");
                }
            case '\\':      /* BRE bound ends with \} */
                if (incon(L_BBND) && next1('}')) {
                    v.now++;
                    intocon(L_BRE);
                    return ret('}');
                } else {
                    throw new RegexException("Errors.REG_BADBR");
                }
            default:
                throw new RegexException("Errors.REG_BADBR");
            }

        case L_BRACK:           /* brackets are not too hard */
            switch (c) {
            case ']':
                if (lasttype('[')) {
                    return retv(Compiler.PLAIN, c);
                } else {
                    intocon(0 != (v.cflags & Flags.REG_EXTENDED) ? L_ERE : L_BRE);
                    return ret(']');
                }
            case '\\':
                note(Flags.REG_UBBS);
                if (0 == (v.cflags & Flags.REG_ADVF)) {
                    return retv(Compiler.PLAIN, c);
                }
                note(Flags.REG_UNONPOSIX);
                if (ateos()) {
                    throw new RegexException("REG_EESCAPE");
                }
                lexescape();

                switch (v.nexttype) {   /* not all escapes okay here */
                case Compiler.PLAIN:
                    return true;

                case Compiler.CCLASS:
                    switch (v.nextvalue) {
                    case 'd':
                        lexnest(brbackd);
                        break;
                    case 's':
                        lexnest(brbacks);
                        break;
                    case 'w':
                        lexnest(brbackw);
                        break;
                    default:
                        throw new RegexException("Errors.REG_EESCAPE");
                    }
                /* lexnest done, back up and try again */
                    v.nexttype = v.lasttype;
                    return next();

                }
            /* not one of the acceptable escapes */
                throw new RegexException("Errors.REG_EESCAPE");

            case '-':
                if (lasttype('[') || next1(']')) {
                    return retv(Compiler.PLAIN, c);
                } else {
                    return retv(Compiler.RANGE, c);
                }

            case '[':
                if (ateos()) {
                    throw new RegexException("Errors.REG_EBRACK");
                }

                switch (charAtNowAdvance()) {
                case '.':
                    intocon(L_CEL);
                /* might or might not be locale-specific */
                    return ret(Compiler.COLLEL);

                case '=':
                    intocon(L_ECL);
                    note(Flags.REG_ULOCALE);
                    return ret(Compiler.ECLASS);

                case ':':
                    intocon(L_CCL);
                    note(Flags.REG_ULOCALE);
                    return ret(Compiler.CCLASS);

                default:            /* oops */
                    v.now--;
                    return retv(Compiler.PLAIN, c);

                }

            default:
                return retv(Compiler.PLAIN, c);

            }

        case L_CEL:         /* collating elements are easy */
            if (c == '.' && next1(']')) {
                v.now++;
                intocon(L_BRACK);
                return retv(Compiler.END, '.');
            } else {
                return retv(Compiler.PLAIN, c);
            }

        case L_ECL:         /* ditto equivalence classes */
            if (c == '=' && next1(']')) {
                v.now++;
                intocon(L_BRACK);
                return retv(Compiler.END, '=');
            } else {
                return retv(Compiler.PLAIN, c);
            }

        case L_CCL:         /* ditto character classes */
            if (c == ':' && next1(']')) {
                v.now++;
                intocon(L_BRACK);
                return retv(Compiler.END, ':');
            } else {
                return retv(Compiler.PLAIN, c);
            }

        default:
            assert false;
            break;
        }

    /* that got rid of everything except EREs and AREs */
        assert incon(L_ERE);

    /* deal with EREs and AREs, except for backslashes */
        switch (c) {
        case '|':
            return ret('|');

        case '*':
            if (0 != (v.cflags & Flags.REG_ADVF) && next1('?')) {
                v.now++;
                note(Flags.REG_UNONPOSIX);
                return retv('*', 0);
            }
            return retv('*', 1);

        case '+':
            if (0 != (v.cflags & Flags.REG_ADVF) && next1('?')) {
                v.now++;
                note(Flags.REG_UNONPOSIX);
                return retv('+', 0);
            }
            return retv('+', 1);

        case '?':
            if (0 != (v.cflags & Flags.REG_ADVF) && next1('?')) {
                v.now++;
                note(Flags.REG_UNONPOSIX);
                return retv('?', 0);
            }
            return retv('?', 1);

        case '{':       /* bounds start or plain character */
            if (0 != (v.cflags & Flags.REG_EXPANDED)) {
                skip();
            }
            if (ateos() || !iscdigit(charAtNow())) {
                note(Flags.REG_UBRACES);
                note(Flags.REG_UUNSPEC);
                return retv(Compiler.PLAIN, c);
            } else {
                note(Flags.REG_UBOUNDS);
                intocon(L_EBND);
                return ret('{');
            }

        case '(':       /* parenthesis, or advanced extension */
            if (0 != (v.cflags & Flags.REG_ADVF) && next1('?')) {
                note(Flags.REG_UNONPOSIX);
                v.now++;
                switch (charAtNowAdvance()) {
                case ':':       /* non-capturing paren */
                    return retv('(', 0);

                case '#':       /* comment */
                    while (!ateos() && charAtNow() != ')') {
                        v.now++;
                    }
                    if (!ateos()) {
                        v.now++;
                    }
                    assert v.nexttype == v.lasttype;
                    return next();

                case '=':       /* positive lookahead */
                    note(Flags.REG_ULOOKAHEAD);
                    return retv(Compiler.LACON, 1);

                case '!':       /* negative lookahead */
                    note(Flags.REG_ULOOKAHEAD);
                    return retv(Compiler.LACON, 0);

                default:
                    throw new RegexException("Errors.REG_BADRPT");

                }
            }
            if (0 != (v.cflags & Flags.REG_NOSUB) || 0 != (v.cflags & Flags.REG_NOCAPT)) {
                return retv('(', 0);        /* all parens non-capturing */
            } else {
                return retv('(', 1);
            }

        case ')':
            if (lasttype('(')) {
                note(Flags.REG_UUNSPEC);
            }
            return retv(')', c);

        case '[':       /* easy except for [[:<:]] and [[:>:]] */
            if (have(6) && charAtNow() == '['
                    && charAtNowPlus(1) == ':'
                    && (charAtNowPlus(2) == '<' || charAtNowPlus(2) == '>')
                    && charAtNowPlus(3) == ':'
                    && charAtNowPlus(4) == ']'
                    && charAtNowPlus(5) == ']') {
                c = charAtNowPlus(2);
                v.now += 6;
                note(Flags.REG_UNONPOSIX);
                return ret((c == '<') ? '<' : '>');
            }
            intocon(L_BRACK);
            if (next1('^')) {
                v.now++;
                return retv('[', 0);
            }
            return retv('[', 1);

        case '.':
            return ret('.');

        case '^':
            return ret('^');

        case '$':
            return ret('$');

        case '\\':      /* mostly punt backslashes to code below */
            if (ateos()) {
                throw new RegexException("REG_EESCAPE");
            }
            break;
        default:        /* ordinary character */
            return retv(Compiler.PLAIN, c);

        }

    /* ERE/ARE backslash handling; backslash already eaten */
        assert !ateos();
        if (0 == (v.cflags & Flags.REG_ADVF)) { /* only AREs have non-trivial escapes */
            if (iscalnum(charAtNow())) {
                note(Flags.REG_UBSALNUM);
                note(Flags.REG_UUNSPEC);
            }
            return retv(Compiler.PLAIN, charAtNowAdvance());
        }

        lexescape();

        if (v.nexttype == Compiler.CCLASS) {    /* fudge at lexical level */
            switch (v.nextvalue) {
            case 'd':
                lexnest(backd);
                break;
            case 'D':
                lexnest(backD);
                break;
            case 's':
                lexnest(backs);
                break;
            case 'S':
                lexnest(backS);
                break;
            case 'w':
                lexnest(backw);
                break;
            case 'W':
                lexnest(backW);
                break;

            default:
                throw new RuntimeException("Invalid escape " + Character.toString((char)v.nextvalue));
            }
        /* lexnest done, back up and try again */
            v.nexttype = v.lasttype;
            return next();
        }
    /* otherwise, lexescape has already done the work */
        return true;
    }
    //CHECKSTYLE:ON

    /**
     * brenext - get next BRE token
     * This is much like EREs except for all the stupid backslashes and the
     */
    boolean brenext(char pc) throws RegexException {
        char c = pc;

        switch (c) {
        case '*':
            if (lasttype(Compiler.EMPTY) || lasttype('(') || lasttype('^')) {
                return retv(Compiler.PLAIN, c);
            }
            return ret('*');
        case '[':
            //CHECKSTYLE:OFF
            if (have(6) && charAtNow() == '[' 
                && charAtNowPlus(1) == ':' 
                && (charAtNowPlus(2) == '<' || charAtNowPlus(2) == '>') 
                &&  charAtNowPlus(3) == ':'
                &&  charAtNowPlus(4) == ']' 
                &&  charAtNowPlus(5) == ']') {
                c = charAtNowPlus(2);
                v.now += 6;
                note(Flags.REG_UNONPOSIX);
                return ret((c == '<') ? '<' : '>');
                //CHECKSTYLE:ON
            }
            intocon(L_BRACK);
            if (next1('^')) {
                v.now++;
                return retv('[', 0);
            }
            return retv('[', 1);
        case '.':
            return ret('.');

        case '^':
            if (lasttype(Compiler.EMPTY)) {
                return ret('^');
            }
            if (lasttype('(')) {
                note(Flags.REG_UUNSPEC);
                return ret('^');
            }
            return retv(Compiler.PLAIN, c);

        case '$':
            if (0 != (v.cflags & Flags.REG_EXPANDED)) {
                skip();
            }
            if (ateos()) {
                return ret('$');
            }
            if (next2('\\', ')')) {
                note(Flags.REG_UUNSPEC);
                return ret('$');
            }
            return retv(Compiler.PLAIN, c);

        case '\\':
            break;      /* see below */
        default:
            return retv(Compiler.PLAIN, c);

        }

        assert c == '\\';

        if (ateos()) {
            throw new RegexException("REG_EESCAPE");
        }

        c = charAtNowAdvance();
        switch (c) {
        case '{':
            intocon(L_BBND);
            note(Flags.REG_UBOUNDS);
            return ret('{');

        case '(':
            return retv('(', 1);

        case ')':
            return retv(')', c);

        case '<':
            note(Flags.REG_UNONPOSIX);
            return ret('<');

        case '>':
            note(Flags.REG_UNONPOSIX);
            return ret('>');

        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            note(Flags.REG_UBACKREF);
            return retv(Compiler.BACKREF, digitval(c));

        default:
            if (iscalnum(c)) {
                note(Flags.REG_UBSALNUM);
                note(Flags.REG_UUNSPEC);
            }
            return retv(Compiler.PLAIN, c);

        }
    }

    void skip() {
        int start = v.now;

        assert 0 != (v.cflags & Flags.REG_EXPANDED);

        for (;;) {
            while (!ateos() && iscspace(charAtNow())) {
                v.now++;
            }
            if (ateos() || charAtNow() != '#') {
                break;              /* NOTE BREAK OUT */
            }
            assert next1('#');
            while (!ateos() && charAtNow() != '\n') {
                v.now++;
            }
        /* leave the newline to be picked up by the iscspace loop */
        }

        if (v.now != start) {
            note(Flags.REG_UNONPOSIX);
        }
    }

    /**
     * lexescape - parse an ARE backslash escape (backslash already eaten)
     * Note slightly nonstandard use of the CCLASS type code.
     */
    //CHECKSTYLE:OFF
    boolean lexescape() throws RegexException {
        char c;
        int save;

        assert 0 != (v.cflags & Flags.REG_ADVF);

        assert !ateos();
        c = charAtNowAdvance();
        if (!iscalnum(c)) {
            return retv(Compiler.PLAIN, c);
        }

        note(Flags.REG_UNONPOSIX);
        switch (c) {
        case 'a':
            return retv(Compiler.PLAIN, '\007');

        case 'A':
            return retv(Compiler.SBEGIN, 0);

        case 'b':
            return retv(Compiler.PLAIN, '\b');

        case 'B':
            return retv(Compiler.PLAIN, '\\');

        case 'c':
            note(Flags.REG_UUNPORT);
            if (ateos()) {
                throw new RegexException("REG_EESCAPE");
            }
            return retv(Compiler.PLAIN, (char)(charAtNowAdvance() & 037));

        case 'd':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 'd');

        case 'D':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 'D');

        case 'e':
            note(Flags.REG_UUNPORT);
            return retv(Compiler.PLAIN, '\033');

        case 'f':
            return retv(Compiler.PLAIN, '\f');

        case 'm':
            return ret('<');

        case 'M':
            return ret('>');

        case 'n':
            return retv(Compiler.PLAIN, '\n');

        case 'r':
            return retv(Compiler.PLAIN, '\r');

        case 's':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 's');

        case 'S':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 'S');

        case 't':
            return retv(Compiler.PLAIN, '\t');

        case 'u':
            c = lexdigits(16, 4, 4);
            return retv(Compiler.PLAIN, c);

        case 'U':
            c = lexdigits(16, 8, 8);
            return retv(Compiler.PLAIN, c);

        case 'v':
            return retv(Compiler.PLAIN, '\u000b');

        case 'w':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 'w');

        case 'W':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.CCLASS, 'W');

        case 'x':
            note(Flags.REG_UUNPORT);
            c = lexdigits(16, 1, 255);  /* REs >255 long outside spec */
            return retv(Compiler.PLAIN, c);

        case 'y':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.WBDRY, 0);

        case 'Y':
            note(Flags.REG_ULOCALE);
            return retv(Compiler.NWBDRY, 0);

        case 'Z':
            return retv(Compiler.SEND, 0);

        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
            save = v.now;
            v.now--;    /* put first digit back */
            c = lexdigits(10, 1, 255);  /* REs >255 long outside spec */
        /* ugly heuristic (first test is "exactly 1 digit?") */
            if (v.now - save == 0 || (int)c <= v.getSubs().size()) {
                note(Flags.REG_UBACKREF);
                return retv(Compiler.BACKREF, (char)c);
            }
        /* oops, doesn't look like it's a backref after all... */
            v.now = save;
        /* and fall through into octal number */
        case '0':
            note(Flags.REG_UUNPORT);
            v.now--;    /* put first digit back */
            c = lexdigits(8, 1, 3);

            return retv(Compiler.PLAIN, c);

        default:
            assert iscalpha(c);
            throw new RegexException("REG_EESCAPE"); // unknown escape.
        }
    }
    //CHECKSTYLE:ON

    /*
 - lexdigits - slurp up digits and return chr value
 ^ static chr lexdigits(struct vars *, int, int, int);
 */
    char            /* chr value; errors signalled via ERR */
    lexdigits(int base, int minlen, int maxlen) throws RegexException {
        int n;          /* unsigned to avoid overflow misbehavior */
        int len;
        char c;
        int d;
        final char ub = (char)base;

        n = 0;
        for (len = 0; len < maxlen && !ateos(); len++) {
            c = charAtNowAdvance();
            switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                d = digitval(c);
                break;
            case 'a':
            case 'A':
                d = 10;
                break;
            case 'b':
            case 'B':
                d = 11;
                break;
            case 'c':
            case 'C':
                d = 12;
                break;
            case 'd':
            case 'D':
                d = 13;
                break;
            case 'e':
            case 'E':
                d = 14;
                break;
            case 'f':
            case 'F':
                d = 15;
                break;
            default:
                v.now--;    /* oops, not a digit at all */
                d = -1;
                break;
            }

            if (d >= base) {    /* not a plausible digit */
                v.now--;
                d = -1;
            }
            if (d < 0) {
                break;      /* NOTE BREAK OUT */
            }
            n = n * ub + d;
        }
        if (len < minlen) {
            throw new RegexException("REG_EESCAPE");
        }

        return (char)n;
    }
}
