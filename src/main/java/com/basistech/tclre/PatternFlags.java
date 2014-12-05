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
 * Flags that accompany a regular expression and modulate its interpretation.
 */
public enum PatternFlags {
    /**
     * Basic Regular Expression.
     */
    BASIC,
    /**
     * Advanced Regular Expression.
     */
    EXTENDED,
    /**
     * Advanced Regular Expression.
     */
    ADVANCED,
    /**
     * Not a regular expression at all; no interpretation of RE syntax.
     */
    QUOTE,
    /**
     * Case-insensitive matching.
     */
    ICASE,
    /**
     * Groups never capture.
     */
    NOSUB,
    /**
     * Expanded syntax where whitespace and comments are ignored. This is the same as specifying the (?x) embedded option.
     */
    EXPANDED,
    /**
     * Changes the behavior of `[^' bracket expressions and `.' so that they stop at newlines. This is the same as specifying the (?p) embedded option.
     */
    NLSTOP,
    /**
     * Changes the behavior of `^' and `$' (the ``anchors'') so they match the beginning and end of a line respectively. This is the same as specifying the (?w) embedded option.
     */
    NLANCH;
}
