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
package org.sunnycode.huff;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.sunnycode.huff.io.BitInput;
import org.sunnycode.huff.io.BitOutput;
import org.sunnycode.huff.model.HuffCode;
import org.sunnycode.huff.model.TextModel;

public class HuffmanCodec {
    public static byte[] encode(byte[] toencode, TextModel d) {
        BitOutput out = new BitOutput(toencode.length * 16);

        int i = 0;
        while (i < toencode.length) {
            if (i + 2 < toencode.length) {
                int tri = toencode[i + 2];
                tri <<= 8;
                tri |= toencode[i + 1];
                tri <<= 8;
                tri |= toencode[i];

                if (d.getDictionary().containsKey(tri)) {
                    out.writeBits(d.getDictionary().get(tri).getTarget(), d
                            .getDictionary().get(tri).getTargetNumBits());

                    i += 3;
                    continue;
                }
            }

            if (i + 1 < toencode.length) {
                int tri = toencode[i + 1];
                tri <<= 8;
                tri |= toencode[i];

                if (d.getDictionary().containsKey(tri)) {
                    out.writeBits(d.getDictionary().get(tri).getTarget(), d
                            .getDictionary().get(tri).getTargetNumBits());

                    i += 2;
                    continue;
                }
            }

            int tri = toencode[i];

            if (d.getDictionary().containsKey(tri)) {
                out.writeBits(d.getDictionary().get(tri).getTarget(), d
                        .getDictionary().get(tri).getTargetNumBits());

                i += 1;
                continue;
            }

            throw new RuntimeException("Character encoding failed! "
                    + new String(toencode) + " at "
                    + new String(toencode).substring(i) + " (" + i + ")");
        }

        out.writeBits(d.getDictionary().get(TextModel.EOF).getTarget(), d
                .getDictionary().get(TextModel.EOF).getTargetNumBits());

        return out.finish();
    }

    public static byte[] decode(byte[] inBytes, TextModel d) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Map<Long, HuffCode> decode = d.getDecode();
        BitInput in = new BitInput(inBytes);
        int maxBits = d.getMaxBits();

        while (true) {
            HuffCode matched = matchDecode(in, decode, maxBits);

            if (matched == null) {
                throw new HuffmanEncodingException(
                        "Huffman decode failed to match token");
            }

            in.advance(matched.getTargetNumBits());

            int sym = matched.getSource();

            if (matched.getSource() == TextModel.EOF) {
                break;
            }

            while (sym != 0) {
                baos.write(sym & 0xFF);
                sym >>= 8;
            }
        }

        return baos.toByteArray();
    }

    private static HuffCode matchDecode(BitInput in,
            Map<Long, HuffCode> decode, int maxBits) {
        for (int i = 1; i <= maxBits; i++) {
            long val = ((long) i << 56) | in.peek(i);
            if (decode.containsKey(val)) {
                return decode.get(val);
            }
        }

        return null;
    }
}
