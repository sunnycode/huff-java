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

import java.io.File;
import java.util.Scanner;

import org.sunnycode.huff.HuffmanCodec;
import org.sunnycode.huff.model.TextModel;

public class AnalyzeCompression {
    public static void main(String[] args) throws Exception {
        for (String file : args) {
            System.out.println("tt: " + System.currentTimeMillis());

            Scanner scan1 = new Scanner(new File(file));
            TextModel.Builder b = new TextModel.Builder(Integer.parseInt(System
                    .getProperty("c", "3")));

            while (scan1.hasNextLine()) {
                b.scanLine(scan1.nextLine());
            }

            Scanner scan2 = new Scanner(new File(file));
            TextModel d = b.build();

            System.out.println("tt: " + System.currentTimeMillis());

            long tc = 0;
            long ec = 0;

            while (scan2.hasNextLine()) {
                String line = scan2.nextLine();
                byte[] orig = line.getBytes();
                byte[] enc2 = HuffmanCodec.encode(orig, d);
                String dec2 = new String(HuffmanCodec.decode(enc2, d));

                if (!dec2.equals(line)) {
                    System.out.println("---------------------");
                    System.out.println(" enc: " + bytesToBits(enc2));
                    System.out.println("ouch: " + line);
                    System.out.println("  ne: " + dec2);
                }

                tc += line.length();
                ec += enc2.length;

                System.out.println("o="
                        + line.length()
                        + ",e="
                        + enc2.length
                        + ",save="
                        + (1 - ((double) enc2.length)
                                / ((double) line.length())));
            }

            System.out.println("------------------");
            System.out.println("o=" + tc + ",e=" + ec + ",save="
                    + (1 - ((double) ec) / ((double) tc)));
            System.out.println("dict size: " + d.getDictionary().size());
            System.out.println("tt: " + System.currentTimeMillis());
        }
    }

    public static String bytesToBits(byte[] toencode) {
        StringBuilder encoded = new StringBuilder();
        String padding = "00000000";

        for (int i = 0; i < toencode.length; i++) {
            String toAppend = Integer.toString(toencode[i] & 0xFF, 2);

            int pad = 8 - (toAppend.length() % 8);
            if (pad < 8) {
                encoded.append(padding.substring(0, pad));
            }

            encoded.append(toAppend);
        }

        return encoded.toString();
    }
}
