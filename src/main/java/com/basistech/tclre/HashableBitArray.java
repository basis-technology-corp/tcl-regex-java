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

/**
 * Created by bsg on 8/9/14.
 */
public class HashableBitArray {
    private boolean[] bits;

    public HashableBitArray(int n) {
        bits = new boolean[n];
    }

    @Override
    public int hashCode() {
        int answer = 0;
        for (int pos = 0, coef = 1; pos < bits.length; pos += 1) {
            if (bits[pos]) {
                answer += coef;
            }
            if (coef == (2 << 31)) {
                coef = 1;
            } else {
                coef <<= 1;
            }
        }
        return answer;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof HashableBitArray)) {
            return false;
        }
        HashableBitArray hba2 = (HashableBitArray)obj;
        if (bits.length != hba2.getLength()) {
            return false;
        }
        for (int i = 0; i < bits.length; i++ ) {
            if (bits[i] != hba2.get(i)) {
                return false;
            }
        }
        return true;
    }

    private int getLength() {
        return bits.length;
    }

    public boolean get(int i) {
        return bits[i];
    }

    public void put(int i, boolean value) {
        bits[i] = value;
    }

}
