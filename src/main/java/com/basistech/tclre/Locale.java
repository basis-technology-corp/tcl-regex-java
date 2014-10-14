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

import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.UnicodeSet;

import it.unimi.dsi.fastutil.objects.Object2CharMap;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;

/**
 * Code from locale.
 */
final class Locale {

/* ASCII character-name table */

    static final Object2CharMap<String> CNAME;
    static final LoadingCache<String, UnicodeSet> KNOWN_SETS_CS = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<String, UnicodeSet>() {
                        public UnicodeSet load(String cclass) throws RegexException {
                            String className = "[:" + cclass + ":]";
                            try {
                                return new UnicodeSet(className, UnicodeSet.ADD_CASE_MAPPINGS);
                            }  catch (IllegalArgumentException iae) {
                                throw new RegexException("Invalid character class name " + cclass);
                            }
                        }
                    });
    static final LoadingCache<String, UnicodeSet> KNOWN_SETS_CI = CacheBuilder.newBuilder()
            .build(
                    new CacheLoader<String, UnicodeSet>() {
                        public UnicodeSet load(String cclass) throws RegexException {
                            String className = "[:" + cclass + ":]";
                            try {
                                return new UnicodeSet(className, 0);
                            }  catch (IllegalArgumentException iae) {
                                throw new RegexException("Invalid character class name " + cclass);
                            }
                        }
                    });

    //CHECKSTYLE:OFF
    static {
        CNAME = new Object2CharOpenHashMap<String>();

        CNAME.put("NUL", '\0');
        CNAME.put("SOH", '\001');
        CNAME.put("STX", '\002');
        CNAME.put("ETX", '\003');
        CNAME.put("EOT", '\004');
        CNAME.put("ENQ", '\005');
        CNAME.put("ACK", '\006');
        CNAME.put("BEL", '\007');
        CNAME.put("alert", '\007');
        CNAME.put("BS", '\010');
        CNAME.put("backspace", '\b');
        CNAME.put("HT", '\011');
        CNAME.put("tab", '\t');
        CNAME.put("LF", '\012');
        CNAME.put("newline", '\n');
        CNAME.put("VT", '\013');
        CNAME.put("vertical-tab", '\u000b');
        CNAME.put("FF", '\014');
        CNAME.put("form-feed", '\f');
        CNAME.put("CR", '\015');
        CNAME.put("carriage-return", '\r');
        CNAME.put("SO", '\016');
        CNAME.put("SI", '\017');
        CNAME.put("DLE", '\020');
        CNAME.put("DC1", '\021');
        CNAME.put("DC2", '\022');
        CNAME.put("DC3", '\023');
        CNAME.put("DC4", '\024');
        CNAME.put("NAK", '\025');
        CNAME.put("SYN", '\026');
        CNAME.put("ETB", '\027');
        CNAME.put("CAN", '\030');
        CNAME.put("EM", '\031');
        CNAME.put("SUB", '\032');
        CNAME.put("ESC", '\033');
        CNAME.put("IS4", '\034');
        CNAME.put("FS", '\034');
        CNAME.put("IS3", '\035');
        CNAME.put("GS", '\035');
        CNAME.put("IS2", '\036');
        CNAME.put("RS", '\036');
        CNAME.put("IS1", '\037');
        CNAME.put("US", '\037');
        CNAME.put("space", ' ');
        CNAME.put("exclamation-mark", '!');
        CNAME.put("quotation-mark", '"');
        CNAME.put("number-sign", '#');
        CNAME.put("dollar-sign", '$');
        CNAME.put("percent-sign", '%');
        CNAME.put("ampersand", '&');
        CNAME.put("apostrophe", '\'');
        CNAME.put("left-parenthesis", '(');
        CNAME.put("right-parenthesis", ')');
        CNAME.put("asterisk", '*');
        CNAME.put("plus-sign", '+');
        CNAME.put("comma", ',');
        CNAME.put("hyphen", '-');
        CNAME.put("hyphen-minus", '-');
        CNAME.put("period", '.');
        CNAME.put("full-stop", '.');
        CNAME.put("slash", '/');
        CNAME.put("solidus", '/');
        CNAME.put("zero", '0');
        CNAME.put("one", '1');
        CNAME.put("two", '2');
        CNAME.put("three", '3');
        CNAME.put("four", '4');
        CNAME.put("five", '5');
        CNAME.put("six", '6');
        CNAME.put("seven", '7');
        CNAME.put("eight", '8');
        CNAME.put("nine", '9');
        CNAME.put("colon", ':');
        CNAME.put("semicolon", ';');
        CNAME.put("less-than-sign", '<');
        CNAME.put("equals-sign", '=');
        CNAME.put("greater-than-sign", '>');
        CNAME.put("question-mark", '?');
        CNAME.put("commercial-at", '@');
        CNAME.put("left-square-bracket", '[');
        CNAME.put("backslash", '\\');
        CNAME.put("reverse-solidus", '\\');
        CNAME.put("right-square-bracket", ']');
        CNAME.put("circumflex", '^');
        CNAME.put("circumflex-accent", '^');
        CNAME.put("underscore", '_');
        CNAME.put("low-line", '_');
        CNAME.put("grave-accent", '`');
        CNAME.put("left-brace", '{');
        CNAME.put("left-curly-bracket", '{');
        CNAME.put("vertical-line", '|');
        CNAME.put("right-brace", '}');
        CNAME.put("right-curly-bracket", '}');
        CNAME.put("tilde", '~');
        CNAME.put("DEL", '\177');
    }
    //CHECKSTYLE:ON


    private Locale() {
        //
    }


    static int element(String what) throws RegexException {
        // this is a single character or the name of a character.
        // Surrogate pairs? we can't deal yet. This function
        // returns 'int' but no one upstairs is home yet.

        if (what.length() == 1) {
            return what.charAt(0);
        }

        if (CNAME.containsKey(what)) {
            return CNAME.get(what);
        }

        int uc = UCharacter.getCharFromName(what); // what if someone names a non-BMP char?
        if (uc != -1) {
            if (uc > 0xffff) {
                throw new RegexException(String.format(
                        "Limitation: cannot handle equivalence outside of the BMP: %s not possible.",
                        what));
            }
            return uc;
        }

        return -1;
    }

    /**
     * eclass - Because we have no MCCE support, this
     * just processing single characters.
     */
    static UnicodeSet eclass(char c, boolean cases) {

    /* otherwise, none */
        if (cases) {
            return allcases(c);
        } else {
            UnicodeSet set = new UnicodeSet();
            set.add(c);
            return set;
        }
    }

    /**
     * allcases - supply cvec for all case counterparts of a chr (including itself)
     * This is a shortcut, preferably an efficient one, for simple characters;
     * messy cases are done via range().
     */
    static UnicodeSet allcases(char c) {
        UnicodeSet set = new UnicodeSet();
        set.add(c);
        set.closeOver(UnicodeSet.ADD_CASE_MAPPINGS);
        return set;
    }

    /**
     * Return a UnicodeSet for a character class name.
     * It appears that the names that TCL accepts are also acceptable to ICU.
     *
     * @param cclassName class name
     * @param casefold whether to include casefolding
     * @return set
     */
    public static UnicodeSet cclass(String cclassName, boolean casefold) throws RegexException {
        try {
            if (casefold) {
                return KNOWN_SETS_CI.get(cclassName);
            } else {
                return KNOWN_SETS_CS.get(cclassName);
            }
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), RegexException.class);
            throw new RegexRuntimeException(e.getCause());
        }
    }
}
