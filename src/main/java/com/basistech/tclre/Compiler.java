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

import com.google.common.collect.Lists;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * from regcomp.c
 */
class Compiler {
    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);

    /* token type codes, some also used as NFA arc types */
    static final int EMPTY = 'n';		/* no token present */
    static final int EOS = 'e';		/* end of string */
    static final int PLAIN = 'p';		/* ordinary character */
    static final int DIGIT = 'd';		/* digit (in bound) */
    static final int BACKREF = 'b';		/* back reference */
    static final int COLLEL = 'I';		/* start of [. */
    static final int ECLASS = 'E';		/* start of [= */
    static final int CCLASS = 'C';		/* start of [: */
    static final int END = 'X';		/* end of [. [= [: */
    static final int RANGE = 'R';		/* - within [] which might be range delim. */
    static final int LACON = 'L';		/* lookahead constraint subRE */
    static final int AHEAD = 'a';		/* color-lookahead arc */
    static final int BEHIND = 'r';		/* color-lookbehind arc */
    static final int WBDRY = 'w';		/* word boundary constraint */
    static final int NWBDRY = 'W';		/* non-word-boundary constraint */
    static final int SBEGIN = 'A';		/* beginning of string (even if not BOL) */
    static final int SEND = 'Z';		/* end of string (even if not EOL) */
    static final int PREFER = 'P';		/* length preference */

    RegExp re;
    char[] pattern;
    int now;		/* scan pointer into string */
    int stop;		/* end of string */
    char[] savepattern;
    int savenow;		/* saved now and stop for "subroutine call" */
    int savestop;
    int err;		/* error code (0 if none) */
    int cflags;		/* copy of compile flags */
    int lasttype;		/* type of previous token */
    int nexttype;		/* type of next token */
    int nextvalue;		/* value (if any) of next token */
    int lexcon;		/* lexical context type (see lex.c) */
    int nsubexp;		/* subexpression count */
    List<Subre> subs;	/* subRE pointer vector */
    int nsubs;		/* length of vector */
    Nfa nfa;	/* the NFA */
    ColorMap cm;	/* character color map */
    short nlcolor;		/* color of newline */
    State wordchrs;	/* state in nfa holding word-char outarcs */
    Subre tree;	/* subexpression tree */
    Subre treechain;	/* all tree nodes allocated */
    Subre treefree;		/* any free tree nodes */
    int ntree;		/* number of tree nodes */
    Cvec cv;	/* interface cvec */
    Cvec cv2;	/* utility cvec */

    Cvec mcces;	/* collating-element information */

    State mccepbegin;	/* in nfa, start of MCCE prototypes */
    State mccepend;	/* in nfa, end of MCCE prototypes */
    List<Subre> lacons;	/* lookahead-constraint vector */
    Lex lex;

    /**
     * Constructor does minimal setup; construct, then call compile().
     * The entire effect is a side-effect on 're'.
     * @param re
     * @param pattern
     * @param flags
     */
    Compiler(RegExp re, String pattern, int flags) {
        if (re == null) {
            throw new NullPointerException();
        }

        if ( (0 != (flags & Flags.REG_QUOTE)) &&
                (0 != (flags&(Flags.REG_ADVANCED|Flags.REG_EXPANDED|Flags.REG_NEWLINE)))) {
            throw new IllegalArgumentException("Invalid flag combination");
        }

        if (0 == (flags&Flags.REG_EXTENDED) && 0 != (flags&Flags.REG_ADVF)) {
            throw new IllegalArgumentException("Invalid flag combination (!extended && advf");
        }

        this.re = re;
        this.pattern = pattern.toCharArray();
        this.cflags = flags;
        subs = Lists.newArrayListWithCapacity(10);
        lacons = Lists.newArrayList();
        // the lexer is 'over there' but shared state here, for now at least.
        lex = new Lex(this);
    }



    int nlacons() {
        return lacons.size();
    }
    // int nlacons;		/* size of lacons */

    char newline() {
        return '\n';
    }
    
    void cnoerr() throws RegexException {
        if (err != 0) {
            throw new RegexException();
        }
    }
    
    boolean see(int t) {
        return nexttype == t;
    }


    void compile() throws RegexException {
        stop = pattern.length;
        nlcolor = Constants.COLORLESS;
        re.info = 0;
        re.csize = 2;
        re.guts = new Guts();

        cm = new ColorMap(this);
        cv = new Cvec(100, 20, 10);

        // No MCESS support, so no initialization of it.

        if (err != 0) {
            throw new RegexException(); // TODO: fix up.
        }

        /* Parsing */

        lex.lexstart();
        if (0 != (cflags & Flags.REG_NLSTOP) || 0 != (cflags& Flags.REG_NLANCH)) {
        /* assign newline a unique color */
            nlcolor = cm.subcolor(newline());
            cm.okcolors(nfa);
        }
        cnoerr();
        tree = parse(EOS, PLAIN, nfa.init, nfa.finalState);
        assert see(EOS);		/* even if error; ISERR() => see(EOS) */

        cnoerr();

        assert tree != null;

    /* finish setup of nfa and its subre tree */
        nfa.specialcolors();

        cnoerr();

        LOG.debug("========= RAW ==========");
        nfa.dumpnfa();
        //dumpst(tree, debug, 1);

        optst(tree);
        ntree = numst(tree, 1);
        markst(tree);
        cleanst();
        LOG.debug("========= TREE FIXED ==========");

    /* build compacted NFAs for tree and lacons */
        re.info |= nfatree(tree);
        cnoerr();

        for (int i = 0; i < lacons.size(); i++) {
            LOG.debug(String.format("========= LA%d ==========", i));
            nfanode(lacons.get(0));
        }

        cnoerr();

        if (0 != (tree.flags & Subre.SHORTER)) {
            note(Flags.REG_USHORTEST);
        }

    /* build compacted NFAs for tree, lacons, fast search */
        LOG.debug("========= SEARCH ==========");
    /* can sacrifice main NFA now, so use it as work area */
        optimize(nfa);
        cnoerr();
        makesearch(nfa);
        cnoerr();
        re.guts.search = compact(nfa);
        cnoerr();

    /* looks okay, package it up */
        re.nsub = nsubexp;

        re.guts.cflags = cflags;
        re.guts.info = re.info;
        re.guts.nsub = re.nsub;
        re.guts.tree = tree;
        re.guts.ntree = ntree;
        if (0 != (cflags & Flags.REG_ICASE)) {
            // TODO: fill in comparators.
        } else {

        }

        re.guts.lacons = lacons;

        assert err == 0;
    }

    /**
     * makesearch - turn an NFA into a search NFA (implicit prepend of .*?)
     * NFA must have been optimize()d already.
     */
    void makesearch(Nfa nfa) {

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
        treefree = null;		/* just on general principles */
    }

    /**
     * nfatree - turn a subRE subtree into a tree of compacted NFAs
     */
    long			/* optimize results from top node */
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
     */
    long			/* optimize results */
    nfanode(Subre t) throws RegexException {
        long ret = 0;

        assert t.begin != null;

        LOG.debug(String.format("========= TREE NODE %s ==========", t));

        Nfa newNfa = new Nfa(nfa);
        if (err != 0) {
            throw new RegexException();
        }

        newNfa.dupnfa(t.begin, t.end, newNfa.init, newNfa.finalState);
        if (!iserr()) {
            newNfa.specialcolors();
            ret = optimize(newNfa);
        }
        if (!iserr()) {
            t.cnfa = compact(newNfa);
        }

        // freenfa ... depend on our friend the GC.
        return ret;
    }


    Subre parse(int stopper, int type, State initState, State finalState) {
        return null;
    }

    /**
     * optimize - optimize an NFA
     */
    long			/* re_info bits */
    optimize(Nfa nfa) {
        return 0;
    }

    /** compact - compact an NFA
     */
    Cnfa compact(Nfa nfa) {
        return null;
    }

    /**
     * Always return false because there is no mcess support enabled.
     * @param c
     * @return
     */
    boolean isCeLeader(char c) {
        return mcces != null && mcces.haschr(c);
    }

    boolean iserr() {
        return err != 0;
    }

    int err(int e) {
        nexttype = EOS;
        if (err != 0) {
            err = e;
        }
        return err;
    }

    void insist(boolean v, int e) {
        if (!v) {
            err(e);
        }
    }

    boolean note(long b) {
        return re.info != b;
    }

    // The C version we use has no collation support generated in, so we get
    // the following stub functions.
    /*
     - nmcces - how many distinct MCCEs are there?
     ^ static int nmcces(struct vars *);
     */
    int nmcces() {
    /*
     * No multi-character collating elements defined at the moment.
     */
        return 0;
    }

    /*
     - nleaders - how many chrs can be first chrs of MCCEs?
     ^ static int nleaders(struct vars *);
     */
    int nleaders() {
        return 0;
    }
}
