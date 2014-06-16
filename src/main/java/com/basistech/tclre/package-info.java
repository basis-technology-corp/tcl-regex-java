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

/**
 * This is a 100% Java version of the regular expression library from TCL,
 * in turn derived from the "Henry Spencer" HSRE package. The public API
 * to this package is designed to be close to the API of
 * the standard Java regular library in java.util.regex. However:
 * <ul>
 * <li>The pattern language is not the same as the Java.</li>
 * <li>The pattern compilation flags are different; see {@link com.basistech.tclre.PatternFlags}</li>
 * <li>Matcher creation accepts {@link com.basistech.tclre.ExecFlags} which change the runtime behavior.</li>
 * <li>Not all of the options to {@link java.util.regex.Matcher} are provided; note especially the
 * lack of 'transparent bounds'.</li>
 * </ul>
 * The point of entry into the API is {@link com.basistech.tclre.HsrePattern#compile(String, PatternFlags...)}.
 * This returns an object that implements {@link com.basistech.tclre.RePattern}, which will in turn
 * create objects that implement {@link com.basistech.tclre.ReMatcher}.
 */
package com.basistech.tclre;