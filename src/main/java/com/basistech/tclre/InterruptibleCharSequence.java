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
 * Implementation of {@link java.lang.CharSequence} that checks for {@link Thread#interrupted()}
 * and throws {@link com.basistech.tclre.InterruptedRegexException}.
 */
public class InterruptibleCharSequence implements CharSequence {
    private final char[] data;
    private final int startOffset;
    private final int endOffset;

    /**
     * Construct from some character data.
     * @param data array containing the data.
     * @param startOffset offset of first character.
     * @param endOffset offset after last character.
     */
    public InterruptibleCharSequence(char[] data, int startOffset, int endOffset) {
        this.data = data;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    private void check() {
        if (Thread.interrupted()) {
            throw new InterruptedRegexException(new InterruptedException());
        }
    }

    @Override
    public int length() {
        check();
        return endOffset - startOffset;
    }

    @Override
    public char charAt(int index) {
        check();
        // ArrayIndexOutOfBounds is subclass of IndexOutOfBounds, so no need to check.
        return data[startOffset + index];
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        check();

        // prevent reaching past the end of the data.
        if ((start + startOffset) > endOffset) {
            throw new IndexOutOfBoundsException("Invalid start offset");
        }
        if ((end + startOffset) > endOffset) {
            throw new IndexOutOfBoundsException("Invalid end offset");
        }
        return new InterruptibleCharSequence(data, startOffset + start, startOffset + end);
    }

    @Override
    public String toString() {
        check();
        return new String(data, startOffset, endOffset - startOffset);
    }

    public char[] getData() {
        return data;
    }

    public int getStartOffset() {
        return startOffset;
    }

    public int getEndOffset() {
        return endOffset;
    }
}
