package com.basistech.tclre;

/**
 * The interface for matchers
 */
interface ReMatcher {
    HsrePattern pattern();
    boolean find(int startOffset) throws RegexException;
    boolean find() throws RegexException;
}
