/*
* Copyright 2014 Basis Technology Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


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
