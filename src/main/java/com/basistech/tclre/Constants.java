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
 * Created by benson on 5/29/14.
 */
final class Constants {
    static final int NOCELT = -1;
    static final int CHRBITS = 16;
    static final int CHR_MIN = 0;
    static final int CHR_MAX = 0xffff;
    static final int BYTBITS = 8;
    static final int BYTTAB = 1 << BYTBITS;
    static final int BYTMASK = BYTTAB - 1;
    static final int NBYTS = (CHRBITS + BYTBITS - 1) / BYTBITS;
    static final short COLORLESS = -1;
    static final short NOSUB = COLORLESS;
    static final short WHITE = 0;

    private Constants() {
        //
    }

    static int chr(int c) {
        return c - '0';
    }
}
