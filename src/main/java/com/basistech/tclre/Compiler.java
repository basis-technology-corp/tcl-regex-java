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
import java.util.List;

import com.google.common.collect.Lists;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * from regcomp.c
 */
final class Compiler {
    /* token type codes, some also used as NFA arc types */
    static final int EMPTY = 'n';       /* no token present */
    static final int EOS = 'e';     /* end of string */
    static final int PLAIN = 'p';       /* ordinary character */
    static final int DIGIT = 'd';       /* digit (in bound) */
    static final int BACKREF = 'b';     /* back reference */
    static final int COLLEL = 'I';      /* start of [. */
    static final int ECLASS = 'E';      /* start of [= */
    static final int CCLASS = 'C';      /* start of [: */
    static final int END = 'X';     /* end of [. [= [: */
    static final int RANGE = 'R';       /* - within [] which might be range delim. */
    static final int LACON = 'L';       /* lookahead constraint subRE */
    static final int AHEAD = 'a';       /* color-lookahead arc */
    static final int BEHIND = 'r';      /* color-lookbehind arc */
    static final int WBDRY = 'w';       /* word boundary constraint */
    static final int NWBDRY = 'W';      /* non-word-boundary constraint */
    static final int SBEGIN = 'A';      /* beginning of string (even if not BOL) */
    static final int SEND = 'Z';        /* end of string (even if not EOL) */
    static final int PREFER = 'P';      /* length preference */
    static final int DUPMAX = 255;
    static final int INFINITY = 256;
    static final int SOME = 2;
    static final int INF = 3;
    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);
    char[] pattern;
    int now;        /* scan pointer into string */
    int stop;       /* end of string */
    char[] savepattern;
    int savenow = -1;        /* saved now and stop for "subroutine call" */
    int savestop = -1;
    int cflags;     /* copy of compile flags */
    int lasttype;       /* type of previous token */
    int nexttype;       /* type of next token */
    int nextvalue;      /* value (if any) of next token */
    int lexcon;     /* lexical context type (see lex.c) */
    List<Subre> subs;   /* subRE pointer vector */
    Nfa nfa;    /* the NFA */
    ColorMap cm;    /* character color map */
    short nlcolor;      /* color of newline */
    State wordchrs; /* state in nfa holding word-char outarcs */
    Subre tree; /* subexpression tree */
    Subre treechain;    /* all tree nodes allocated */
    Subre treefree;     /* any free tree nodes */
    int ntree;      /* number of tree nodes */
    List<Subre> lacons; /* lookahead-constraint vector */
    Lex lex;
    private long info;
    private final EnumSet<PatternFlags> originalFlags;

    /**
     * Constructor does minimal setup; construct, then call compile().
     * The entire effect is a side-effect on 're'.
     *
     * @param pattern
     * @param flags
     */
    private Compiler(String pattern, EnumSet<PatternFlags> flags) {

        if (flags.contains(PatternFlags.QUOTE)
                && (flags.contains(PatternFlags.ADVANCED)
                    || flags.contains(PatternFlags.EXPANDED)
                    || flags.contains(PatternFlags.NLANCH)
                    || flags.contains(PatternFlags.NLSTOP))) {
            throw new IllegalArgumentException("Invalid flag combination");
        }

        this.pattern = pattern.toCharArray();
        this.originalFlags = flags;

        // Map from EnumSet, which is how we want users to see this some time, to bitflags.
        // At some point we might push the enum sets all the way down.
        for (PatternFlags f : flags) {
            switch (f) {
            case BASIC:
                this.cflags |= Flags.REG_BASIC;
                break;
            case EXTENDED:
                this.cflags |= Flags.REG_EXPANDED;
                break;
            case ADVANCED:
                this.cflags |= Flags.REG_ADVF;
                this.cflags |= Flags.REG_EXTENDED;
                break;
            case QUOTE:
                this.cflags |= Flags.REG_QUOTE;
                break;
            case ICASE:
                this.cflags |= Flags.REG_ICASE;
                break;
            case NOSUB:
                this.cflags |= Flags.REG_NOSUB;
                break;
            case EXPANDED:
                this.cflags |= Flags.REG_EXPANDED;
                break;
            case NLSTOP:
                this.cflags |= Flags.REG_NLSTOP;
                break;
            case NLANCH:
                this.cflags |= Flags.REG_NLANCH;
                break;
            default:
                throw new RuntimeException("Can't handle " + f);
            }

        }

        subs = Lists.newArrayListWithCapacity(10);
        lacons = Lists.newArrayList();
        // the lexer is 'over there' but shared state here, for now at least.
        lex = new Lex(this);
    }

    /**
     * The official API into this class.
     * @param pattern the pattern
     * @param flags the flags
     * @return the regexp
     * @throws RegexException
     */
    static RePattern compile(String pattern, EnumSet<PatternFlags> flags) throws RegexException {
        Compiler that = new Compiler(pattern, flags);
        return that.compile();
    }

    private RePattern compile() throws RegexException {
        stop = pattern.length;
        nlcolor = Constants.COLORLESS;
        info = 0;
        Guts guts = new Guts();

        cm = new ColorMap(this);
        nfa = new Nfa(cm);

        // No MCESS support, so no initialization of it.

        /* Parsing */

        lex.lexstart();
        if (0 != (cflags & Flags.REG_NLSTOP) || 0 != (cflags & Flags.REG_NLANCH)) {
        /* assign newline a unique color */
            nlcolor = cm.subcolor(newline());
            cm.okcolors(nfa);
        }

        tree = parse(EOS, PLAIN, nfa.init, nfa.finalState);
        assert see(EOS);        /* even if error; ISERR() => see(EOS) */


        assert tree != null;

    /* finish setup of nfa and its subre tree */
        nfa.specialcolors();

        if (LOG.isDebugEnabled()) {
            LOG.debug("========= RAW ==========");
            nfa.dumpnfa();
            LOG.debug(tree.dumpst(true));
        }

        optst(tree);
        ntree = numst(tree, 1);
        markst(tree);
        cleanst();

        if (LOG.isDebugEnabled()) {
            LOG.debug("========= TREE FIXED ==========");
            LOG.debug(tree.dumpst(true));
        }

    /* build compacted NFAs for tree and lacons */
        info |= nfatree(tree);

        // lacons start at 1.
        for (int i = 1; i < lacons.size(); i++) {
            LOG.debug(String.format("========= LA%d ==========", i));
            nfanode(lacons.get(i));
        }

        if (0 != (tree.flags & Subre.SHORTER)) {
            note(Flags.REG_USHORTEST);
        }

    /* build compacted NFAs for tree, lacons, fast search */
        if (LOG.isDebugEnabled()) {
            LOG.debug("========= SEARCH ==========");
        }
    /* can sacrifice main NFA now, so use it as work area */
        nfa.optimize();
        makesearch(nfa);
        guts.search = nfa.compact();

    /* looks okay, package it up */
        int nsub = subs.size();
        guts.cm = cm;
        guts.cflags = cflags;
        guts.info = info;
        guts.nsub = nsub;
        guts.tree = tree;
        guts.ntree = ntree;
        if (0 != (cflags & Flags.REG_ICASE)) {
            guts.compare = new Comparer(true);
        } else {
            guts.compare = new Comparer(false);
        }

        guts.lacons = lacons;
        return new HsrePattern(new String(pattern, 0, pattern.length), originalFlags, info, nsub, guts);
    }

    static int pair(int a, int b) {
        return a * 4 + b;
    }

    static int reduce(int x) {
        if (x == INFINITY) {
            return INF;
        } else if (x > 1) {
            return SOME;
        } else {
            return x;
        }
    }

    char newline() {
        return '\n';
    }

    boolean see(int t) {
        return nexttype == t;
    }

    static class Comparer implements SubstringComparator {
        private UTF16.StringComparator comparator;

        Comparer(boolean caseInsensitive) {
            if (caseInsensitive) {
                comparator = new UTF16.StringComparator(true, true,
                        UTF16.StringComparator.FOLD_CASE_DEFAULT);
            } else {
                comparator = new UTF16.StringComparator(true, false,
                        UTF16.StringComparator.FOLD_CASE_DEFAULT);
            }
        }

        @Override
        public int compare(CharSequence data, int start1, int start2, int length) {
            String s1 = data.subSequence(start1, start1 + length).toString();
            String s2 = data.subSequence(start2, start2 + length).toString();
            return comparator.compare(s1, s2);
        }
    }

    /**
     * makesearch - turn an NFA into a search NFA (implicit prepend of .*?)
     * NFA must have been optimize()d already.
     */
    void makesearch(Nfa nfa) {
        Arc a;
        Arc b;
        State pre = nfa.pre;
        State s;
        State s2;
        State slist;

    /* no loops are needed if it's anchored */
        for (a = pre.outs; a != null; a = a.outchain) {
            assert a.type == PLAIN;
            if (a.co != nfa.bos[0] && a.co != nfa.bos[1]) {
                break;
            }
        }
        if (a != null) {
        /* add implicit .* in front */
            cm.rainbow(nfa, PLAIN, Constants.COLORLESS, pre, pre);

        /* and ^* and \A* too -- not always necessary, but harmless */
            nfa.newarc(PLAIN, nfa.bos[0], pre, pre);
            nfa.newarc(PLAIN, nfa.bos[1], pre, pre);
        }

    /*
     * Now here's the subtle part.  Because many REs have no lookback
     * constraints, often knowing when you were in the pre state tells
     * you little; it's the next state(s) that are informative.  But
     * some of them may have other inarcs, i.e. it may be possible to
     * make actual progress and then return to one of them.  We must
     * de-optimize such cases, splitting each such state into progress
     * and no-progress states.
     */

    /* first, make a list of the states */
        slist = null;
        for (a = pre.outs; a != null; a = a.outchain) {
            s = a.to;
            for (b = s.ins; b != null; b = b.inchain) {
                if (b.from != pre) {
                    break;
                }
            }
            if (b != null) {        /* must be split */
                if (s.tmp == null) {  /* if not already in the list */
                                   /* (fixes bugs 505048, 230589, */
                                   /* 840258, 504785) */
                    s.tmp = slist;
                    slist = s;
                }
            }
        }

    /* do the splits */
        for (s = slist; s != null; s = s2) {
            s2 = nfa.newstate();
            copyouts(nfa, s, s2);
            for (a = s.ins; a != null; a = b) {
                b = a.inchain;
                if (a.from != pre) {
                    cparc(nfa, a, a.from, s2);
                    nfa.freearc(a);
                }
            }
            s2 = s.tmp;
            s.tmp = null;       /* clean up while we're at it */
        }
    }

    /**
     * findarc - find arc, if any, from given source with given type and color
     * If there is more than one such arc, the result is random.
     */
    Arc findarc(State s, int type, short co) {
        Arc a;

        for (a = s.outs; a != null; a = a.outchain) {
            if (a.type == type && a.co == co) {
                return a;
            }
        }
        return null;
    }

    /**
     * - cparc - allocate a new arc within an NFA, copying details from old one
     * ^ static VOID cparc(struct nfa *, struct arc *, struct state *,
     * ^    struct state *);
     */
    void cparc(Nfa nfa, Arc oa, State from, State to) {
        nfa.newarc(oa.type, oa.co, from, to);
    }

    /**
     * - moveins - move all in arcs of a state to another state
     * You might think this could be done better by just updating the
     * existing arcs, and you would be right if it weren't for the desire
     * for duplicate suppression, which makes it easier to just make new
     * ones to exploit the suppression built into newarc.
     */
    void moveins(Nfa nfa, State old, State newState) {
        Arc a;

        assert old != newState;

        while ((a = old.ins) != null) {
            cparc(nfa, a, a.from, newState);
            nfa.freearc(a);
        }
        assert old.nins == 0;
        assert old.ins == null;
    }

    /**
     * copyins - copy all in arcs of a state to another state
     */
    void copyins(Nfa nfa, State old, State newState) {
        Arc a;

        assert old != newState;

        for (a = old.ins; a != null; a = a.inchain) {
            cparc(nfa, a, a.from, newState);
        }
    }

    /**
     * moveouts - move all out arcs of a state to another state
     * ^ static VOID moveouts(struct nfa *, struct state *, struct state *);
     */
    void moveouts(Nfa nfa, State old, State newState) {
        Arc a;

        assert old != newState;

        while ((a = old.outs) != null) {
            cparc(nfa, a, newState, a.to);
            nfa.freearc(a);
        }
    }

    /**
     * copyouts - copy all out arcs of a state to another state
     */
    void copyouts(Nfa nfa, State old, State newState) {
        Arc a;

        assert old != newState;

        for (a = old.outs; a != null; a = a.outchain) {
            cparc(nfa, a, newState, a.to);
        }
    }

    /**
     * cloneouts - copy out arcs of a state to another state pair, modifying type
     */
    void cloneouts(Nfa nfa, State old, State from, State to, int type) {
        Arc a;

        assert old != from;

        for (a = old.outs; a != null; a = a.outchain) {
            nfa.newarc(type, a.co, from, to);
        }
    }

    /**
     * optst - optimize a subRE subtree
     */
    void optst(Subre t) {
        if (t == null) {
            return;
        }

    /* recurse through children */
        if (t.left != null) {
            optst(t.left);
        }
        if (t.right != null) {
            optst(t.right);
        }
    }

    /**
     * numst - number tree nodes (assigning retry indexes)
     *
     * @return next number
     */
    int numst(Subre t, int start) {
        int i;

        assert t != null;

        i = start;
        t.retry = (short)i++;

        if (t.left != null) {
            i = numst(t.left, i);
        }
        if (t.right != null) {
            i = numst(t.right, i);
        }
        return i;
    }

    /**
     * markst - mark tree nodes as INUSE
     */
    void markst(Subre t) {
        assert t != null;

        t.flags |= Subre.INUSE;
        if (t.left != null) {
            markst(t.left);
        }
        if (t.right != null) {
            markst(t.right);
        }
    }

    /**
     * cleanst - free any tree nodes not marked INUSE
     */
    void cleanst() {
        treechain = null;
        treefree = null;        /* just on general principles */
    }

    /**
     * nfatree - turn a subRE subtree into a tree of compacted NFAs
     */
    long            /* optimize results from top node */
    nfatree(Subre t) throws RegexException {
        assert t != null && t.begin != null;

        if (t.left != null) {
            nfatree(t.left);
        }
        if (t.right != null) {
            nfatree(t.right);
        }
        return nfanode(t);
    }

    /**
     * nfanode - do one NFA for nfatree
     *
     * @return results of {@link Nfa#optimize()}
     */
    long nfanode(Subre t) throws RegexException {
        long ret;

        assert t.begin != null;

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("========= TREE NODE %s ==========", t.shortId()));
        }

        Nfa newNfa = new Nfa(nfa);
        newNfa.dupnfa(t.begin, t.end, newNfa.init, newNfa.finalState);
        newNfa.specialcolors();
        ret = newNfa.optimize();
        t.cnfa = newNfa.compact();

        // freenfa ... depend on our friend the GC.
        return ret;
    }

    int lmix(int f) {
        return f << 2;  /* LONGER -> MIXED */
    }

    int smix(int f) {
        return f << 1;  /* SHORTER -> MIXED */
    }

    int up(int f) {
        return (f & ~Subre.LOCAL) | (lmix(f) & smix(f) & Subre.MIXED);
    }

    boolean eat(char t) throws RegexException {
        return see(t) && lex.next();
    }

    boolean messy(int f) {
        return 0 != (f & (Subre.MIXED | Subre.CAP | Subre.BACKR));
    }

    /**
     * parse - parse an RE
     * This is actually just the top level, which parses a bunch of branches
     * tied together with '|'.  They appear in the tree as the left children
     * of a chain of '|' subres.
     */
    Subre parse(int stopper, int type, State initState, State finalState) throws RegexException {
        State left; /* scaffolding for branch */
        State right;
        Subre branches; /* top level */
        Subre branch;   /* current branch */
        Subre t;    /* temporary */
        int firstbranch;    /* is this the first branch? */

        assert stopper == ')' || stopper == EOS;

        branches = new Subre('|', Subre.LONGER, initState, finalState);

        branch = branches;
        firstbranch = 1;
        do {    /* a branch */
            if (0 == firstbranch) {
            /* need a place to hang it */
                branch.right = new Subre('|', Subre.LONGER, initState, finalState);
                branch = branch.right;
            }
            firstbranch = 0;
            left = nfa.newstate();
            right = nfa.newstate();

            nfa.emptyarc(initState, left);
            nfa.emptyarc(right, finalState);

            branch.left = parsebranch(stopper, type, left, right, false);

            branch.flags |= up(branch.flags | branch.left.flags);
            if ((branch.flags & ~branches.flags) != 0)  /* new flags */ {
                for (t = branches; t != branch; t = t.right) {
                    t.flags |= branch.flags;
                }
            }
        } while (eat('|'));
        assert see(stopper) || see(EOS);

        if (!see(stopper)) {
            assert stopper == ')' && see(EOS);
            //ERR(REG_EPAREN);
            throw new RegexException("REG_EPAREN");
        }

    /* optimize out simple cases */
        if (branch == branches) {   /* only one branch */
            assert branch.right == null;
            t = branch.left;
            branch.left = null;
            branches = t;
        } else if (!messy(branches.flags)) {    /* no interesting innards */
            branches.left = null;
            branches.right = null;
            branches.op = '=';
        }

        return branches;
    }

    /**
     * parsebranch - parse one branch of an RE
     * This mostly manages concatenation, working closely with parseqatom().
     * Concatenated things are bundled up as much as possible, with separate
     * ',' nodes introduced only when necessary due to substructure.
     */
    Subre parsebranch(int stopper, int type, State left, State right, boolean partial) throws RegexException {
        State lp;   /* left end of current construct */
        boolean seencontent = false;    /* is there anything in this branch yet? */
        Subre t;

        lp = left;

        t = new Subre('=', 0, left, right); /* op '=' is tentative */
        while (!see('|') && !see(stopper) && !see(EOS)) {
            if (seencontent) {  /* implicit concat operator */
                lp = nfa.newstate();
                moveins(nfa, right, lp);
            }
            seencontent = true;

        /* NB, recursion in parseqatom() may swallow rest of branch */
            parseqatom(stopper, type, lp, right, t);
        }

        if (!seencontent) {     /* empty branch */
            if (!partial) {
                note(Flags.REG_UUNSPEC);
            }

            assert lp == left;
            nfa.emptyarc(left, right);
        }

        return t;
    }

    //CHECKSTYLE:OFF
    void parseqatom(int stopper, int type, State lp, State rp, Subre top) throws RegexException {
        State s;    /* temporaries for new states */
        State s2;
        int m;
        int n;
        Subre atom; /* atom's subtree */
        Subre t;

        boolean cap;    /* capturing parens? */
        int pos;        /* positive lookahead? */
        int subno;      /* capturing-parens or backref number */
        int atomtype;
        int qprefer;        /* quantifier short/long preference */
        int f;
        AtomSetter atomp = null;

    /* initial bookkeeping */
        atom = null;
        assert lp.nouts == 0;   /* must string new code */
        assert rp.nins == 0;    /*  between lp and rp */
        subno = 0;      /* just to shut lint up */

    /* an atom or constraint... */
        atomtype = nexttype;
        switch (atomtype) {
    /* first, constraints, which end by returning */
        case '^':
            nfa.newarc('^', (short)1, lp, rp);
            if (0 != (cflags & Flags.REG_NLANCH)) {
                nfa.newarc(BEHIND, nlcolor, lp, rp);
            }
            lex.next();
            return;

        case '$':
            nfa.newarc('$', (short)1, lp, rp);
            if (0 != (cflags & Flags.REG_NLANCH)) {
                nfa.newarc(AHEAD, nlcolor, lp, rp);
            }
            lex.next();
            return;

        case SBEGIN:
            nfa.newarc('^', (short)1, lp, rp);  /* BOL */
            nfa.newarc('^', (short)0, lp, rp);  /* or BOS */
            lex.next();
            return;

        case SEND:
            nfa.newarc('$', (short)1, lp, rp);  /* EOL */
            nfa.newarc('$', (short)0, lp, rp);  /* or EOS */
            lex.next();
            return;

        case '<':
            wordchrs(); /* does next() */
            s = nfa.newstate();
            nonword(BEHIND, lp, s);
            word(AHEAD, s, rp);
            return;

        case '>':
            wordchrs(); /* does next() */
            s = nfa.newstate();
            word(BEHIND, lp, s);
            nonword(AHEAD, s, rp);
            return;

        case WBDRY:
            wordchrs(); /* does next() */
            s = nfa.newstate();
            nonword(BEHIND, lp, s);
            word(AHEAD, s, rp);
            s = nfa.newstate();
            word(BEHIND, lp, s);
            nonword(AHEAD, s, rp);
            return;

        case NWBDRY:
            wordchrs(); /* does next() */
            s = nfa.newstate();
            word(BEHIND, lp, s);
            word(AHEAD, s, rp);
            s = nfa.newstate();
            nonword(BEHIND, lp, s);
            nonword(AHEAD, s, rp);
            return;

        case LACON: /* lookahead constraint */
            pos = nextvalue;
            lex.next();
            s = nfa.newstate();
            s2 = nfa.newstate();
            parse(')', LACON, s, s2); // parse for side-effect.
            assert see(')');
            lex.next();
            n = newlacon(s, s2, pos);
            nfa.newarc(LACON, (short)n, lp, rp);
            return;

    /* then errors, to get them out of the way */
        case '*':
        case '+':
        case '?':
        case '{':
            throw new RegexException("REG_BADRPT");

    /* then plain characters, and minor variants on that theme */
        case ')':       /* unbalanced paren */
            if ((cflags & Flags.REG_ADVANCED) != Flags.REG_EXTENDED) {
                throw new RegexException("REG_EPAREN");
            }
        /* legal in EREs due to specification botch */
            note(Flags.REG_UPBOTCH);
        /* fallthrough into case PLAIN */
        case PLAIN:
            onechr((char)nextvalue, lp, rp);
            cm.okcolors(nfa);
            lex.next();
            break;
        case '[':
            if (nextvalue == 1) {
                bracket(lp, rp);
            } else {
                cbracket(lp, rp);
            }
            assert see(']');
            lex.next();
            break;
        case '.':
            cm.rainbow(nfa, PLAIN,
                    (0 != (cflags & Flags.REG_NLSTOP)) ? nlcolor : Constants.COLORLESS,
                    lp, rp);
            lex.next();
            break;
    /* and finally the ugly stuff */
        case '(':   /* value flags as capturing or non */
            if (type == LACON) {
                cap = false;
            } else {
                cap = nextvalue != 0;
            }
            if (cap) {
                subno = subs.size() + 1; // first subno is 1.
            } else {
                atomtype = PLAIN;   /* something that's not '(' */
            }
            lex.next();
        /* need new endpoints because tree will contain pointers */
            s = nfa.newstate();
            s2 = nfa.newstate();

            nfa.emptyarc(lp, s);
            nfa.emptyarc(s2, rp);

            atom = parse(')', PLAIN, s, s2);
            assert see(')');
            lex.next();

            if (cap) {
                assert subs.size() == subno - 1;
                subs.add(atom);
                t = new Subre('(', atom.flags | Subre.CAP, lp, rp);
                t.subno = subno;
                t.left = atom;
                atom = t;
            }
        /* postpone everything else pending possible {0} */
            break;
        case BACKREF:   /* the Feature From The Black Lagoon */
            if (type == LACON) {
                throw new RegexException("REG_ESUBREG");
            }
            if (nextvalue > subs.size()) {
                throw new RegexException(String.format("Backreference to %d out of range of defined subexpressions (%d)", nextvalue, subs.size()));
            }
            if (subs.get(nextvalue - 1) == null) { // \1 is first backref, living in slot 0.
                throw new RegexException(String.format("Backreference to %d refers to non-capturing group.", nextvalue));
            }

            assert nextvalue > 0;
            atom = new Subre('b', Subre.BACKR, lp, rp);
            subno = nextvalue ;
            atom.subno = subno;
            nfa.emptyarc(lp, rp);   /* temporarily, so there's something */
            lex.next();
            break;

        default:
            throw new RuntimeException("Impossible type in lex");
        }

    /* ...and an atom may be followed by a quantifier */
        switch (nexttype) {
        case '*':
            m = 0;
            n = INFINITY;
            qprefer = (nextvalue != 0) ? Subre.LONGER : Subre.SHORTER;
            lex.next();
            break;
        case '+':
            m = 1;
            n = INFINITY;
            qprefer = (nextvalue != 0) ? Subre.LONGER : Subre.SHORTER;
            lex.next();
            break;
        case '?':
            m = 0;
            n = 1;
            qprefer = (nextvalue != 0) ? Subre.LONGER : Subre.SHORTER;
            lex.next();
            break;
        case '{':
            lex.next();
            m = scannum();
            if (eat(',')) {
                if (see(DIGIT)) {
                    n = scannum();
                } else {
                    n = INFINITY;
                }
                if (m > n) {
                    throw new RegexException("First quantity is larger than second quantity in {m,n} quantifier.");
                }
            /* {m,n} exercises preference, even if it's {m,m} */
                qprefer = (nextvalue != 0) ? Subre.LONGER : Subre.SHORTER;
            } else {
                n = m;
            /* {m} passes operand's preference through */
                qprefer = 0;
            }
            if (!see('}')) {    /* catches errors too */
                throw new RegexException("Invalid syntax for {m,n} quantifier.");
            }
            lex.next();
            break;

        default:        /* no quantifier */
            m = 1;
            n = 1;
            qprefer = 0;
            break;
        }

    /* annoying special case:  {0} or {0,0} cancels everything */
        if (m == 0 && n == 0) {
            if (atomtype == '(') {
                assert subno == subs.size() - 1;
                subs.remove(subs.size() - 1);
            }
            delsub(nfa, lp, rp);
            nfa.emptyarc(lp, rp);
            return;
        }

    /* if not a messy case, avoid hard part */
        assert !messy(top.flags);
        f = top.flags | qprefer | ((atom != null) ? atom.flags : 0);
        if (atomtype != '(' && atomtype != BACKREF && !messy(up(f))) {
            if (!(m == 1 && n == 1)) {
                repeat(lp, rp, m, n);
            }
            top.flags = f;
            return;
        }

    /*
     * hard part:  something messy
     * That is, capturing parens, back reference, short/long clash, or
     * an atom with substructure containing one of those.
     */

    /* now we'll need a subre for the contents even if they're boring */
        if (atom == null) {
            atom = new Subre('=', 0, lp, rp);
        }

    /*
     * prepare a general-purpose state skeleton
     *
     *    --. [s] ---prefix--. [begin] ---atom--. [end] ----rest--. [rp]
     *   /                                            /
     * [lp] ---. [s2] ----bypass---------------------
     *
     * where bypass is an empty, and prefix is some repetitions of atom
     */
        s = nfa.newstate();     /* first, new endpoints for the atom */
        s2 = nfa.newstate();
        nfa.moveouts(lp, s);
        nfa.moveins(rp, s2);
        atom.begin = s;
        atom.end = s2;
        s = nfa.newstate();     /* and spots for prefix and bypass */
        s2 = nfa.newstate();
        nfa.emptyarc(lp, s);
        nfa.emptyarc(lp, s2);

    /* break remaining subRE into x{...} and what follows */
        t = new Subre('.', Subre.combine(qprefer, atom.flags), lp, rp);
        t.left = atom;

        final Subre target = t;
        atomp = new AtomSetter() {

            @Override
            public void set(Subre s) {
                target.left = s;
            }
        };

    /* here we should recurse... but we must postpone that to the end */

    /* split top into prefix and remaining */
        assert top.op == '=' && top.left == null && top.right == null;
        top.left = new Subre('=', top.flags, top.begin, lp);
        top.op = '.';
        top.right = t;

    /* if it's a backref, now is the time to replicate the subNFA */
        if (atomtype == BACKREF) {
            assert atom.begin.nouts == 1; /* just the EMPTY */
            delsub(nfa, atom.begin, atom.end);
            assert subs.get(subno - 1) != null;
        /* and here's why the recursion got postponed:  it must */
        /* wait until the skeleton is filled in, because it may */
        /* hit a backref that wants to copy the filled-in skeleton */
            nfa.dupnfa(subs.get(subno - 1).begin, subs.get(subno - 1).end,
                    atom.begin, atom.end);
        }

    /* it's quantifier time; first, turn x{0,...} into x{1,...}|empty */
        if (m == 0) {
            nfa.emptyarc(s2, atom.end);     /* the bypass */
            assert Subre.pref(qprefer) != 0;
            f = Subre.combine(qprefer, atom.flags);
            t = new Subre('|', f, lp, atom.end);
            t.left = atom;
            t.right = new Subre('|', Subre.pref(f), s2, atom.end);
            t.right.left = new Subre('=', 0, s2, atom.end);

            atomp.set(t);
            final Subre target2 = t;
            atomp = new AtomSetter() {
                @Override
                public void set(Subre s) {
                    target2.left = s;
                }
            };
            m = 1;
        }

    /* deal with the rest of the quantifier */
        if (atomtype == BACKREF) {
        /* special case:  backrefs have internal quantifiers */
            nfa.emptyarc(s, atom.begin);    /* empty prefix */
        /* just stuff everything into atom */
            repeat(atom.begin, atom.end, m, n);
            atom.min = (short)m;
            atom.max = (short)n;
            atom.flags |= Subre.combine(qprefer, atom.flags);
        } else if (m == 1 && n == 1) {
        /* no/vacuous quantifier:  done */
            nfa.emptyarc(s, atom.begin);    /* empty prefix */
        } else {
        /* turn x{m,n} into x{m-1,n-1}x, with capturing */
        /*  parens in only second x */
            nfa.dupnfa(atom.begin, atom.end, s, atom.begin);
            assert m >= 1 && m != INFINITY && n >= 1;
            repeat(s, atom.begin, m - 1, (n == INFINITY) ? n : n - 1);
            f = Subre.combine(qprefer, atom.flags);
            t = new Subre('.', f, s, atom.end); /* prefix and atom */
            t.left = new Subre('=', Subre.pref(f), s, atom.begin);
            t.right = atom;
            atomp.set(t);
        }

    /* and finally, look after that postponed recursion */
        t = top.right;
        if (!(see('|') || see(stopper) || see(EOS))) {
            t.right = parsebranch(stopper, type, atom.end, rp, true);
        } else {
            nfa.emptyarc(atom.end, rp);
            t.right = new Subre('=', 0, atom.end, rp);
        }
        assert see('|') || see(stopper) || see(EOS);
        t.flags |= Subre.combine(t.flags, t.right.flags);
        top.flags |= Subre.combine(top.flags, t.flags);
    }
    //CHECKSTYLE:ON

    void delsub(Nfa nfa, State lp, State rp) {
        rp.tmp = rp;
        deltraverse(nfa, lp, lp);
        assert lp.nouts == 0 && rp.nins == 0;   /* did the job */
        assert lp.no != State.FREESTATE && rp.no != State.FREESTATE;    /* no more */
        lp.tmp = null;
        rp.tmp = null;
    }

    /**
     * deltraverse - the recursive heart of delsub
     * This routine's basic job is to destroy all out-arcs of the state.
     */
    void deltraverse(Nfa nfa, State leftend, State s) {
        Arc a;
        State to;

        if (s.nouts == 0) {
            return;         /* nothing to do */
        }
        if (s.tmp != null) {
            return;         /* already in progress */
        }

        s.tmp = s;          /* mark as in progress */

        while ((a = s.outs) != null) {
            to = a.to;
            deltraverse(nfa, leftend, to);
            assert to.nouts == 0 || to.tmp != null;
            nfa.freearc(a);
            if (to.nins == 0 && to.tmp == null) {
                assert to.nouts == 0;
                nfa.freestate(to);
            }
        }
        assert s.no != State.FREESTATE; /* we're still here */
        assert s == leftend || s.nins != 0; /* and still reachable */
        assert s.nouts == 0;        /* but have no outarcs */

        s.tmp = null;           /* we're done here */
    }

    /**
     * nonword - generate arcs for non-word-character ahead or behind
     */
    void nonword(int dir, State lp, State rp) {
        int anchor = (dir == AHEAD) ? '$' : '^';

        assert dir == AHEAD || dir == BEHIND;
        nfa.newarc(anchor, (short)1, lp, rp);
        nfa.newarc(anchor, (short)0, lp, rp);
        cm.colorcomplement(nfa, dir, wordchrs, lp, rp);
    /* (no need for special attention to \n) */
    }

    /**
     * word - generate arcs for word character ahead or behind
     */
    void word(int dir, State lp, State rp) {
        assert dir == AHEAD || dir == BEHIND;
        cloneouts(nfa, wordchrs, lp, rp, dir);
    /* (no need for special attention to \n) */
    }

    /**
     * scannum - scan a number
     *
     * @return value <= DUPMAX
     */
    int scannum() throws RegexException {
        int n = 0;

        while (see(DIGIT) && n < DUPMAX) {
            n = n * 10 + nextvalue;
            lex.next();
        }

        if (see(DIGIT) || n > DUPMAX) {
            throw new RegexException("REG_BADBR");
        }
        return n;
    }

    /**
     * repeat - replicate subNFA for quantifiers
     * The duplication sequences used here are chosen carefully so that any
     * pointers starting out pointing into the subexpression end up pointing into
     * the last occurrence.  (Note that it may not be strung between the same
     * left and right end states, however!)  This used to be important for the
     * subRE tree, although the important bits are now handled by the in-line
     * code in parse(), and when this is called, it doesn't matter any more.
     */
    void repeat(State lp, State rp, int m, int n) throws RegexException {
        final int rm = reduce(m);
        final int rn = reduce(n);
        State s;
        State s2;

        switch (pair(rm, rn)) {
        // pair(0, 0)
        case 0:     /* empty string */
            // never get here; other code optimizes this out.
            delsub(nfa, lp, rp);
            nfa.emptyarc(lp, rp);
            break;
        //case PAIR(0, 1):      /* do as x| */
        case 1:
            nfa.emptyarc(lp, rp);
            break;
        //case PAIR(0, SOME):       /* do as x{1,n}| */
        case SOME:
            repeat(lp, rp, 1, n);
            nfa.emptyarc(lp, rp);
            break;
        //case PAIR(0, INF):        /* loop x around */
        case INF:
            s = nfa.newstate();
            nfa.moveouts(lp, s);
            nfa.moveins(rp, s);
            nfa.emptyarc(lp, s);
            nfa.emptyarc(s, rp);
            break;
        //case PAIR(1, 1):      /* no action required */
        case 4 * 1 + 1:
            break;
        //case PAIR(1, SOME):       /* do as x{0,n-1}x = (x{1,n-1}|)x */
        case 4 * 1 + SOME:
            s = nfa.newstate();
            nfa.moveouts(lp, s);
            nfa.dupnfa(s, rp, lp, s);
            repeat(lp, s, 1, n - 1);
            nfa.emptyarc(lp, s);
            break;
        //case PAIR(1, INF):        /* add loopback arc */
        case 4 * 1 + INF:
            s = nfa.newstate();
            s2 = nfa.newstate();
            nfa.moveouts(lp, s);
            nfa.moveins(rp, s2);
            nfa.emptyarc(lp, s);
            nfa.emptyarc(s2, rp);
            nfa.emptyarc(s2, s);
            break;
        //case PAIR(SOME, SOME):        /* do as x{m-1,n-1}x */
        case 4 * SOME + SOME:
            s = nfa.newstate();
            nfa.moveouts(lp, s);
            nfa.dupnfa(s, rp, lp, s);
            repeat(lp, s, m - 1, n - 1);
            break;
        //case PAIR(SOME, INF):     /* do as x{m-1,}x */
        case 4 * SOME + INF:
            s = nfa.newstate();
            nfa.moveouts(lp, s);
            nfa.dupnfa(s, rp, lp, s);
            repeat(lp, s, m - 1, n);
            break;
        default:
            throw new RuntimeException("Impossible quantification");
        }
    }

    /**
     * wordchrs - set up word-chr list for word-boundary stuff, if needed
     * The list is kept as a bunch of arcs between two dummy states; it's
     * disposed of by the unreachable-states sweep in NFA optimization.
     * Does NEXT().  Must not be called from any unusual lexical context.
     * This should be reconciled with the \w etc. handling in lex.c, and
     * should be cleaned up to reduce dependencies on input scanning.
     */
    void wordchrs() throws RegexException {
        State left;
        State right;

        if (wordchrs != null) {
            lex.next();     /* for consistency */
            return;
        }

        left = nfa.newstate();
        right = nfa.newstate();
    /* fine point:  implemented with [::], and lexer will set REG_ULOCALE */
        lex.lexword();
        lex.next();
        assert savepattern != null && see('[');
        bracket(left, right);
        assert savepattern != null && see(']');
        lex.next();
        wordchrs = left;
    }

    /**
     * bracket - handle non-complemented bracket expression
     * Also called from cbracket for complemented bracket expressions.
     */
    void bracket(State lp, State rp) throws RegexException {
        assert see('[');
        lex.next();
        while (!see(']') && !see(EOS)) {
            brackpart(lp, rp);
        }
        assert see(']');
        cm.okcolors(nfa);
    }

    //CHECKSTYLE:OFF
    /**
     * brackpart - handle one item (or range) within a bracket expression
     */
    void brackpart(State lp, State rp) throws RegexException {
        UnicodeSet set;
        char c;
        // start and end positions of the name of something,
        int startp;
        int endp;
        // start and end chars of a range
        char startc;
        char endc = 0;
        int ele;

    /* parse something, get rid of special cases, take shortcuts */
        switch (nexttype) {
        case RANGE:         /* a-b-c or other botch */
            throw new RegexException("REG_ERANGE");
        case PLAIN:
            c = (char)nextvalue;
            lex.next();
        /* shortcut for ordinary chr (not range, not MCCE leader) */
            if (!see(RANGE)) {
                onechr(c, lp, rp);
                return;
            }
            // since element returns the input char for a one-char element,
            // and this is guaranteed to be a 1-char element ...
            startc = c;
            break;
        case COLLEL:
            startp = now;
            endp = scanplain();
            if (endp <= startp) {
                throw new RegexException("REG_ECOLLATE");
            }
            // now we want a character name.
            String charName = new String(pattern, startp, endp - startp);
            ele = Locale.element(charName);
            if (ele == -1) {
                throw new RegexException("Unvalid character name " + charName);
            } else {
                startc = (char)ele;
            }
            break;
        case ECLASS:
            startp = now;
            endp = scanplain();
            if (endp <= startp) {
                throw new RegexException("Unterminated or invalid equivalence class.");
            }
            charName = new String(pattern, startp, endp - startp);
            ele = Locale.element(charName);
            if (ele == -1) {
                throw new RegexException("Invalid character name " + charName);
            } else {
                startc = (char)ele;
            }
            set = Locale.eclass(startc, 0 != (cflags & Flags.REG_ICASE));
            dovec(set, lp, rp);
            return;
        case CCLASS:
            startp = now;
            endp = scanplain();
            if (endp <= startp) {
                throw new RegexException("REG_ECTYPE");
            }
            set = Locale.cclass(new String(pattern, startp, endp - startp), 0 != (cflags & Flags.REG_ICASE));
            dovec(set, lp, rp);
            return;

        default:
            throw new RegexException("REG_ASSERT");
        }

        if (see(RANGE)) {
            lex.next();
            switch (nexttype) {
            case PLAIN:
            case RANGE:
                c = (char)nextvalue;
                lex.next();
                endc = c;
                break;
            case COLLEL:
                startp = now;
                endp = scanplain();
                if (endp <= startp) {
                    throw new RegexException("REG_ECOLLATE");
                }

                // look up named character.
                String charName = new String(pattern, startp, endp - startp);
                ele = Locale.element(charName);
                if (ele == -1) {
                    throw new RegexException("Unvalid character name " + charName);
                }
                break;
            default:
                throw new RegexException("REG_ERANGE");

            }
        } else {
            endc = startc;
        }

    /*
     * Ranges are unportable.  Actually, standard C does
     * guarantee that digits are contiguous, but making
     * that an exception is just too complicated.
     */
        if (startc != endc) {
            note(Flags.REG_UUNPORT);
        }

        set = new UnicodeSet(startc, endc);
        if (0 != (cflags & Flags.REG_ICASE)) {
            set.closeOver(UnicodeSet.ADD_CASE_MAPPINGS);
        }
        dovec(set, lp, rp);
    }
    //CHECKSTYLE:ON

    /**
     * scanplain - scan PLAIN contents of [. etc.
     * Certain bits of trickery in lex.c know that this code does not try
     * to look past the final bracket of the [. etc.
     *
     * @return pos past the sequence
     */
    int scanplain() throws RegexException {
        int endp;

        assert see(COLLEL) || see(ECLASS) || see(CCLASS);
        lex.next();

        endp = now;
        while (see(PLAIN)) {
            endp = now;
            lex.next();
        }

        assert see(END);
        lex.next();

        return endp;
    }

    /**
     * cbracket - handle complemented bracket expression
     * We do it by calling bracket() with dummy endpoints, and then complementing
     * the result.  The alternative would be to invoke rainbow(), and then delete
     * arcs as the b.e. is seen... but that gets messy.
     */
    void cbracket(State lp, State rp) throws RegexException {
        State left = nfa.newstate();
        State right = nfa.newstate();

        bracket(left, right);
        if (0 != (cflags & Flags.REG_NLSTOP)) {
            nfa.newarc(PLAIN, nlcolor, left, right);
        }

        assert lp.nouts == 0;       /* all outarcs will be ours */

    /* easy part of complementing */
        cm.colorcomplement(nfa, PLAIN, left, lp, rp);

        // No MCCE in Java.
        nfa.dropstate(left);
        assert right.nins == 0;
        nfa.freestate(right);
    }

    /**
     * newlacon - allocate a lookahead-constraint subRE
     *
     * @return lacon number
     */
    int newlacon(State begin, State end, int pos) {
        if (lacons.size() == 0) {
            // skip 0
            lacons.add(null);
        }
        Subre sub = new Subre((char)0, 0, begin, end);
        sub.subno = pos;
        lacons.add(sub);
        return lacons.size() - 1; // it's the index into the array, -1.
    }

    /**
     * onechr - fill in arcs for a plain character, and possible case complements
     * This is mostly a shortcut for efficient handling of the common case.
     */
    void onechr(char c, State lp, State rp) throws RegexException {
        if (0 == (cflags & Flags.REG_ICASE)) {
            nfa.newarc(PLAIN, cm.subcolor(c), lp, rp);
            return;
        }

    /* rats, need general case anyway... */
        dovec(Locale.allcases(c), lp, rp);
    }

    /**
     * dovec - fill in arcs for each element of a cvec
     * all kinds of MCCE complexity removed.
     */
    void dovec(UnicodeSet set, State lp, State rp) throws RegexException {

        int rangeCount = set.getRangeCount();
        for (int rx = 0; rx < rangeCount; rx++) {
            int rangeStart = set.getRangeStart(rx);
            int rangeEnd = set.getRangeEnd(rx);
            if (rangeStart == rangeEnd) {
                nfa.newarc(PLAIN, cm.subcolor((char)rangeStart), lp, rp);
            }
            cm.subrange((char)rangeStart, (char)rangeEnd, lp, rp);
        }
    }

    void note(long b) {
        info |= b;
    }

    interface AtomSetter {
        void set(Subre s);
    }
}
