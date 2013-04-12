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

public class BitOutput {
    private static final int LONG_BITS = 64;
    private final ByteBuffer buf;
    private final LongBuffer lbuf;
    private long current;
    private long totalBits;
    private int offset;
    private int remain;
    private boolean closed;

    public BitOutput(int maxSize) {
        this.buf = ByteBuffer.allocateDirect(maxSize);
        this.buf.order(ByteOrder.BIG_ENDIAN);
        this.lbuf = this.buf.asLongBuffer();
        this.current = 0L;
        this.totalBits = 0L;
        this.offset = 0;
        this.remain = LONG_BITS;
    }

    public void writeBits(long value, int numBits) {
        if (this.closed) {
            throw new IllegalStateException("BitOutput already finished!");
        }

        if (numBits == 0) {
            return;
        }

        int newRem = this.remain - numBits;

        if (newRem == 0) {
            this.current |= value;
            this.lbuf.put(this.current);
            this.current = 0L;
            this.offset = 0;
            this.remain = LONG_BITS;
        } else if (newRem > 0) {
            this.current |= value << newRem;
            this.offset += numBits;
            this.remain -= numBits;
        } else {
            // overage < 0
            int hiBits = numBits - this.remain;
            long hiVal = value >>> hiBits;
            long loVal = value << (LONG_BITS - hiBits);

            this.current |= hiVal;
            this.lbuf.put(this.current);
            this.current = loVal;
            this.offset = -1 * newRem;
            this.remain = LONG_BITS - hiBits;
        }

        this.totalBits += numBits;
    }

    public byte[] finish() {
        if (!this.closed) {
            this.closed = true;

            if (this.offset > 0) {
                this.lbuf.put(this.current);
            }
        }

        int todo = (int) this.totalBits >>> 3;

        if ((this.totalBits & 0x7) != 0) {
            todo += 1;
        }

        byte[] toReturn = new byte[todo];
        this.buf.rewind();
        this.buf.get(toReturn);

        return toReturn;
    }
}
