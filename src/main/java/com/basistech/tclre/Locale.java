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

import it.unimi.dsi.fastutil.objects.Object2CharMap;
import it.unimi.dsi.fastutil.objects.Object2CharOpenHashMap;

/**
 * Created by benson on 5/31/14.
 */
final class Locale {

/* ASCII character-name table */

    static final Object2CharMap<String> CNAME;
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


    static Cvec range(char start, char end, boolean cases) {
        return null;
    }

    // from regc_locale

    static int element(String what) {
        if (CNAME.containsKey(what)) {
            return CNAME.get(what);
        }
        return -1;
    }





    private Locale() {
        //
    }
}
