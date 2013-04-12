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
package org.sunnycode.huff.cmd;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.codehaus.jackson.map.ObjectMapper;
import org.sunnycode.huff.model.HuffCode;
import org.sunnycode.huff.model.TextModel;

public class AnalyzeTextModel {
    public static void main(String[] args) throws Exception {
        TextModel.Builder b = new TextModel.Builder(Integer.parseInt(System
                .getProperty("c", "3")));

        System.err.println("tt: " + System.currentTimeMillis());

        ByteBuffer buf = ByteBuffer.allocateDirect(64 * 1024 * 1024);

        for (String file : args) {
            FileInputStream instream = new FileInputStream(file);
            FileChannel channel = instream.getChannel();

            long p = 0;
            long s = channel.read(buf, p);
            while (s >= 0) {
                if (s != 0) {
                    int x = 0;
                    for (; x < s - 8; x += 8) {
                        long y = buf.getLong(x);
                        for (int z = 0; z < 8; z++) {
                            b.scan((int) (0xFF & (y >> (z << 3))));
                        }
                    }
                    for (; x < s; x++) {
                        b.scan((int) buf.get(x));
                    }
                }
                System.err.println("processed: " + s + " bytes");
                p += s;
                buf.rewind();
                s = channel.read(buf, p);
            }
        }

        System.err.println("tt: " + System.currentTimeMillis());

        TextModel d = b.build();

        ObjectMapper mapper = new ObjectMapper();

        for (HuffCode code : d.getDictionary().values()) {
            System.out.println(code.getSource() + "\t" + code.getSourceString()
                    + "\t" + mapper.writeValueAsString(code));
        }

        System.err.println("tt: " + System.currentTimeMillis());
    }
}
