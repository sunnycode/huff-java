package org.sunnycode.huff;

import org.apache.commons.codec.binary.Hex;
import org.sunnycode.huff.io.BitOutput;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class BitOutputTest {
    public void testBitOutput() throws Exception {
        new Tester(8).writeN(8, 1).matchHex("7f");
        new Tester(8).writeN(8, 2).matchHex("1b5f");
        new Tester(8).writeN(8, 4).matchHex("01234567");
        new Tester(8).writeN(8, 8).matchHex("0001020304050607");
        new Tester(16).writeN(8, 16).matchHex(
                "00000001000200030004000500060007");
        new Tester(24).writeN(8, 24).matchHex(
                "000000000001000002000003000004000005000006000007");
        new Tester(24).writeN(64, 3).matchHex(
                "05397725bb7f4d3df76dbfff977977b5fb7fdf7df7ffffff");
    }

    public static class Tester {
        private final BitOutput out;

        public Tester(int maxSize) {
            this.out = new BitOutput(maxSize);
        }

        public Tester writeN(int n, int width) {
            for (int i = 0; i < n; i++) {
                this.writeBits(i, width);
            }

            return this;
        }

        public Tester writeBits(long value, int numBits) {
            this.out.writeBits(value, numBits);

            return this;
        }

        public void match(byte[] value) {
            Assert.assertEquals(value, this.out.finish());
        }

        public void matchHex(String value) throws Exception {
            Assert.assertEquals(Hex.decodeHex(value.toCharArray()),
                    this.out.finish());
        }

        public byte[] get() {
            return this.out.finish();
        }
    }
}
