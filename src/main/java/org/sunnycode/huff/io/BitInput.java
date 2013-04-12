/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sunnycode.huff.io;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;

public class BitInput {
    private static final int LONG_BITS = 64;
    private static final long MASK = 0xFFFFFFFFFFFFFFFFL;

    private final ByteBuffer buf;
    private final LongBuffer lbuf;
    private long current;
    private int lbufOffset;
    private int curRemain;

    public BitInput(byte[] initial) {
        if (initial == null || initial.length == 0) {
            throw new IllegalArgumentException("input length must be non-zero!");
        }

        this.buf = ByteBuffer.allocateDirect(initial.length + 16);
        this.buf.order(ByteOrder.BIG_ENDIAN);
        this.buf.put(initial);
        this.buf.rewind();

        this.lbuf = this.buf.asLongBuffer();
        this.current = lbuf.get();
        this.lbufOffset = 0;
        this.curRemain = LONG_BITS;
    }

    public long peek(int numBits) {
        if (numBits > LONG_BITS) {
            throw new IllegalArgumentException(
                    "NumBits to match must be <= 64!");
        }

        if (numBits == 0) {
            return 0L;
        }

        int curRem = this.curRemain;

        if (curRem == 0) {
            this.current = this.lbuf.get(this.lbufOffset + 1);
            this.curRemain = curRem = LONG_BITS;
        }

        int maskOff = LONG_BITS - numBits;
        long curValue = this.current;

        if (numBits <= curRem) {
            long mask = MASK >>> maskOff;

            return mask & (curValue >>> (curRem - numBits));
        }

        int firstBits = (numBits <= curRem) ? numBits : curRem;
        int secondBits = (numBits > curRem) ? (numBits - curRem) : 0;

        long mask = MASK >>> (LONG_BITS - firstBits);
        long maskedActualValue = mask & curValue;
        maskedActualValue <<= secondBits;

        long secondMask = MASK >>> (LONG_BITS - secondBits);

        maskedActualValue |= secondMask
                & (this.lbuf.get(this.lbufOffset + 1) >>> (LONG_BITS - secondBits));

        return maskedActualValue;
    }

    public boolean matches(long value, int numBits) {
        if (numBits == 0) {
            return true;
        }

        int maskOff = LONG_BITS - numBits;
        long maskedTargetValue = (MASK >>> maskOff) & value;

        return this.peek(numBits) == maskedTargetValue;
    }

    public void advance(int numBits) {
        if (numBits > LONG_BITS) {
            throw new IllegalArgumentException(
                    "NumBits to match must be <= 64!");
        }

        if (numBits < this.curRemain) {
            this.curRemain -= numBits;

            return;
        }

        numBits -= this.curRemain;
        this.lbufOffset += 1;
        this.current = lbuf.get(this.lbufOffset);
        this.curRemain = LONG_BITS - numBits;
    }
}
