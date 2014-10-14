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
