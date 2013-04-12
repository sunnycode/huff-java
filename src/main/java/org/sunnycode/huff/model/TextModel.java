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
package org.sunnycode.huff.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TextModel {
    public static final int EOF = 255;
    private final List<CharFreq> freqList;
    private final CharFreq huffTreeRoot;
    private final Map<Integer, HuffCode> dictionary;
    private final Map<Long, HuffCode> decode;
    private final long tot;
    private final int context;
    private int maxBits;

    public TextModel(Map<Integer, Long> freq, long tot, int context) {
        if (context < 1 || context > 3) {
            throw new IllegalArgumentException(
                    "Error: context must be in range 1-3!");
        }

        this.context = context;
        this.tot = tot;
        this.freqList = sortFreq(freq, tot);
        this.huffTreeRoot = createHuffmanTree(this.freqList);
        this.dictionary = createDictionary(
                new LinkedHashMap<Integer, HuffCode>(), this.huffTreeRoot, "");
        this.maxBits = 0;

        for (HuffCode code : dictionary.values()) {
            if (code.getTargetNumBits() > this.maxBits) {
                this.maxBits = code.getTargetNumBits();
            }
        }

        this.decode = createDecode(dictionary);
    }

    public int getMaxBits() {
        return maxBits;
    }

    public int getContext() {
        return context;
    }

    public long getTot() {
        return tot;
    }

    public Map<Long, HuffCode> getDecode() {
        return decode;
    }

    public Map<Integer, HuffCode> getDictionary() {
        return Collections.unmodifiableMap(this.dictionary);
    }

    private static Map<Long, HuffCode> createDecode(
            Map<Integer, HuffCode> dictionary) {
        LinkedHashMap<Long, HuffCode> map = new LinkedHashMap<Long, HuffCode>();

        for (HuffCode code : dictionary.values()) {
            long key = ((long) code.getTargetNumBits() << 56)
                    | code.getTarget();
            map.put(key, code);
        }

        return map;
    }

    private static Map<Integer, HuffCode> createDictionary(
            Map<Integer, HuffCode> dictionary, CharFreq input, String marker) {
        if (input.b > 0 && marker != null && marker.length() > 0) {
            dictionary.put(input.b, new HuffCode(input.b,
                    byteWidth(input.b) * 8, marker, Long.parseLong(marker, 2),
                    marker.length(), input.f));
        }

        if (input.l != null) {
            createDictionary(dictionary, input.l, marker + "0");
        }

        if (input.r != null) {
            createDictionary(dictionary, input.r, marker + "1");
        }

        return dictionary;
    }

    private static CharFreq createHuffmanTree(List<CharFreq> theInput) {
        List<CharFreq> input = new ArrayList<CharFreq>();
        input.addAll(theInput);

        while (input.size() > 1) {
            CharFreq z = new CharFreq();

            if (input.size() > 0) {
                z.l = input.remove(0);
                z.f = z.l.f;
            }

            if (input.size() > 0) {
                z.r = input.remove(0);
                z.f += z.r.f;
            }

            for (int j = 0; j < input.size(); j++) {
                if (z.f < input.get(j).f) {
                    input.add(j, z);
                    z = null;
                    break;
                }
            }

            if (z != null && z.f >= 0) {
                input.add(z);
            }
        }

        return input.get(0);
    }

    private static List<CharFreq> sortFreq(Map<Integer, Long> freq, long tot) {
        List<CharFreq> output = new ArrayList<CharFreq>();

        for (Map.Entry<Integer, Long> entry : freq.entrySet()) {
            if (entry.getValue() == 0) {
                continue;
            }

            // int byteWidth = byteWidth(entry.getKey());
            // if (byteWidth == 3
            // && (((double) entry.getValue() / (double) tot) < 0.001)) {
            // continue;
            // }
            //
            // if (byteWidth == 2
            // && (((double) entry.getValue() / (double) tot) < 0.01)) {
            // continue;
            // }

            CharFreq toAdd = new CharFreq();

            toAdd.b = entry.getKey();
            toAdd.f = entry.getValue();

            output.add(toAdd);
        }

        Collections.sort(output, new Comparator<CharFreq>() {
            public int compare(CharFreq o1, CharFreq o2) {
                if (o1.f > o2.f) {
                    return 1;
                } else if (o2.f > o1.f) {
                    return -1;
                }

                return 0;
            }
        });

        return output;
    }

    private static int byteWidth(int key) {
        if ((key & 0xFF0000) != 0) {
            return 3;
        } else if ((key & 0xFF00) != 0) {
            return 2;
        }

        return 1;
    }

    public static class CharTrieNode {
        public Map<Integer, CharTrieNode> children = new LinkedHashMap<Integer, CharTrieNode>();
        public CharTrieNode parent = null;
        public int b;
        public long c;
        public long t;

        @Override
        public String toString() {
            return "[b=" + b + ",c=" + c + ",t=" + t + ",children=" + children
                    + "]";
        }
    }

    public static class Builder {
        private final long[] chars = new long[256];
        private final CharTrieNode root = new CharTrieNode();
        private final LinkedList<Integer> context = new LinkedList<Integer>();
        private final int contextLen;
        private int lines;

        public Builder(int contextLen) {
            this.contextLen = contextLen;
            this.lines = 1;
        }

        public void scanLine(String in) {
            for (int i = 0; i < in.length(); i += 1) {
                scan(in.charAt(i));
            }

            lines += 1;

            clearContext();
        }

        public void clearContext() {
            context.clear();
        }

        public void scan(int in) {
            if ((in < 32 || in > 126)
                    && (in != '\t' && in != '\r' && in != '\n')) {
                System.err.println("line " + lines + ", ignoring char: " + in);

                return;
            }

            if (in == '\n') {
                lines += 1;
            }

            chars[in] += 1;

            context.add(in);

            if (context.size() > contextLen) {
                context.removeFirst();
            }

            CharTrieNode current = root;

            for (Integer i : context) {
                if (!current.children.containsKey(i)) {
                    CharTrieNode anew = new CharTrieNode();
                    anew.parent = current;
                    anew.b = i;
                    current.children.put(i, anew);
                }

                current = current.children.get(i);
                current.t += 1;
            }

            current.c += 1;
        }

        public void merge(Builder other) {
            for (int i = 0; i < other.chars.length; i++) {
                this.chars[i] += other.chars[i];
            }

            this.lines += other.lines;

            // TODO: merge context?
            mergeTrie(this.root, other.root);
        }

        private void mergeTrie(CharTrieNode here, CharTrieNode there) {
            here.c += there.c;
            here.t += there.t;

            for (CharTrieNode n : there.children.values()) {
                if (!here.children.containsKey(n.b)) {
                    CharTrieNode anew = new CharTrieNode();
                    anew.parent = here;
                    anew.b = n.b;
                    here.children.put(anew.b, anew);
                }

                mergeTrie(here.children.get(n.b), n);
            }
        }

        public void compact(CharTrieNode node) {
            if (!node.children.isEmpty()) {
                List<Integer> codes = new ArrayList<Integer>();
                codes.addAll(node.children.keySet());

                for (Integer key : codes) {
                    CharTrieNode child = node.children.get(key);

                    // remove children who are smaller than 1/64 of parent
                    if (node.parent != null && node.parent.parent != null
                            && ((child.c << 6) < node.t)) {
                        // System.out.println("---------------------");
                        // System.out.println(" from: " + node);
                        // System.out.println("prune: " + child);
                        node.children.remove(key);
                        node.c += child.c;
                    } else {
                        compact(child);
                    }
                }
            }
        }

        public Map<Integer, Long> getFreq(int depth, int sym,
                CharTrieNode node, Map<Integer, Long> collect) {
            sym |= (node.b << ((depth - 1) * 8));

            if (!node.children.isEmpty()) {
                for (Map.Entry<Integer, CharTrieNode> entry : node.children
                        .entrySet()) {
                    getFreq(depth + 1, sym, entry.getValue(), collect);
                }
            } else {
                collect.put(sym, node.t);
            }

            return collect;
        }

        public TextModel build() {
            CharTrieNode eof = new CharTrieNode();
            eof.b = EOF;
            eof.c = lines;
            eof.t = lines;
            eof.parent = root;

            root.children.put(EOF, eof);

            // compact(root);

            Map<Integer, Long> fr = new LinkedHashMap<Integer, Long>();
            for (int i = 0; i < chars.length; i++) {
                if (chars[i] > 0) {
                    fr.put(i, chars[i]); // chars[i] /* div 64 */);
                }
            }

            Map<Integer, Long> f = getFreq(0, 0, root, fr);

            return new TextModel(f, root.t, contextLen);
        }
    }

    /**
     * Huffman Tree node
     */
    public static class CharFreq {
        public int b;
        public long f;
        public CharFreq l;
        public CharFreq r;
        public String code;
    }
}
