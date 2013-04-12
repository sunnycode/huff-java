package org.sunnycode.huff;

import org.sunnycode.huff.io.BitInput;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class BitInputTest {
    public void testBitInput() {
        Tester t0 = new Tester(new byte[] { (byte) 0xFF, 0x00 });
        t0.match(true, 0L, 0);
        t0.match(true, 1L, 0);
        t0.advance(8);
        t0.match(true, 0L, 0);
        t0.match(true, 1L, 0);

        Tester t1 = new Tester(new byte[] { 0x00 });
        t1.match(true, 0L, 8);
        t1.match(true, 0L, 64);
        t1.match(false, 1L, 8);
        t1.match(false, -1L, 8);
        t1.match(false, 1L, 64);

        Tester t2 = new Tester(new byte[] { (byte) 0xF0 });
        t2.match(true, 0x0F, 1);
        t2.match(true, 0x0F, 4);
        t2.match(true, 0xF0, 8);
        t2.match(false, 0L, 64);

        Tester t3 = new Tester(new byte[] { (byte) 0xFF });
        t3.match(true, 0xFFL, 1);
        t3.match(true, 0xFFL, 8);
        t3.match(true, -1L, 8);
        t3.match(false, 0L, 64);
    }

    public static class Tester {
        private final BitInput in;

        public Tester(byte[] input) {
            this.in = new BitInput(input);
        }

        public void advance(int numBits) {
            this.in.advance(numBits);
        }

        public void match(boolean truth, long value, int numBits) {
            Assert.assertEquals(in.matches(value, numBits), truth,
                    "bad match: " + truth + " v=" + value + ",n=" + numBits);
        }
    }
}
