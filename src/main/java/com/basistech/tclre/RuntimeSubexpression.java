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
package com.basistech.tclre;

import java.io.Serializable;

/**
 * Information needed at runtime for a subexpression.
 */
final class RuntimeSubexpression implements Serializable {
    static final long serialVersionUID = 1L;
    final int number;
    final Cnfa machine;
    final char op;
    final RuntimeSubexpression left;
    final RuntimeSubexpression right;
    final int flags;
    final int retry;
    final int min;
    final int max;

    RuntimeSubexpression() {
        number = -1;
        machine = null;
        op = 0;
        left = null;
        right = null;
        flags = 0;
        retry = 0;
        this.min = 0;
        this.max = 0;
    }

    RuntimeSubexpression(Subre re) {
        this.number = re.subno;
        this.op = re.op;
        this.machine = re.cnfa;
        this.flags = re.flags;
        if (re.left == null) {
            this.left = null;
        } else {
            this.left = new RuntimeSubexpression(re.left);
        }
        if (re.right == null) {
            this.right = null;
        } else {
            this.right = new RuntimeSubexpression(re.right);
        }
        this.retry = re.retry;
        this.min = re.min;
        this.max = re.max;
    }
}
