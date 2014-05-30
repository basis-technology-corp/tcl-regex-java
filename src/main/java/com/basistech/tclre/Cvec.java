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
import it.unimi.dsi.fastutil.chars.CharArrayList;
import it.unimi.dsi.fastutil.chars.CharList;

import java.util.List;

/**
 * Character vectors.
 * These use fastutil to be infinitely expandable.
 */
class Cvec {
    static final int MAXMCCE = 2;
    CharList chrs; // this might want to be a set.
    CharList ranges;
    List<String> mcces;

    Cvec(int nchrs, int nranges, int nmcces) {
        chrs = new CharArrayList(nchrs);
        ranges = new CharArrayList(nranges * 2);
        mcces = Lists.newArrayListWithCapacity(nmcces);
    }

    Cvec clearcvec() {
        chrs.clear();
        mcces.clear();
        ranges.clear();
        return this;
    }

    void addchr(char c) {
        chrs.add(c);
    }

    void addrange(char from, char to) {
        ranges.add(from);
        ranges.add(to);
    }

    void addmcce(char[] data, int start, int end) {
        if (data == null) {
            return;
        }
        int len = end - start;
        assert len > 0;
        mcces.add(new String(data, start, len));
    }

    /**
     * Return true if the char is either listed explicitly or covered in a range.
     * @param c the char
     * @return true if present.
     */
    boolean haschr(char c) {
        if (chrs.contains(c)) {
            return true;
        }

        for (int rangeIndex = 0; rangeIndex < ranges.size(); rangeIndex += 2) {
            if (c >= ranges.getChar(rangeIndex) && c <= ranges.getChar(rangeIndex + 1)) {
                return true;
            }
        }
        return false;
    }
}
