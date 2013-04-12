package org.sunnycode.huff.model;

import java.nio.ByteBuffer;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Huffman encoding table entry
 */
public class HuffCode {
    private final int source;
    private final int sourceNumBits;
    private final int targetNumBits;
    private final String targetString;
    private final long target;
    private final long frequency;

    public HuffCode(@JsonProperty("s") int source,
            @JsonProperty("sn") int sourceNumBits,
            @JsonProperty("ts") String targetString,
            @JsonProperty("t") long target,
            @JsonProperty("tn") int targetNumBits,
            @JsonProperty("f") long frequency) {
        this.source = source;
        this.sourceNumBits = sourceNumBits;
        this.target = target;
        this.targetNumBits = targetNumBits;
        this.targetString = targetString;
        this.frequency = frequency;
    }

    @JsonProperty("s")
    public int getSource() {
        return source;
    }

    @JsonProperty("ss")
    public String getSourceString() {
        return outVal(source);
    }

    @JsonProperty("sx")
    public String getSourceHex() {
        return Integer.toHexString(source);
    }

    @JsonProperty("sn")
    public int getSourceNumBits() {
        return sourceNumBits;
    }

    @JsonProperty("t")
    public long getTarget() {
        return target;
    }

    @JsonProperty("tn")
    public int getTargetNumBits() {
        return targetNumBits;
    }

    @JsonProperty("ts")
    public String getTargetString() {
        return targetString;
    }

    @JsonProperty("f")
    public long getFrequency() {
        return frequency;
    }

    private String outVal(int input) {
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.asIntBuffer().put(input);

        StringBuilder b = new StringBuilder();

        for (int i = 0; i < 4; i++) {
            char c = (char) buf.get(i);
            if (c == 0) {
                continue;
            }

            if (c >= ' ' && c < 127) {
                b.append((char) buf.get(i));
            } else {
                b.append("?");
            }
        }

        return b.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof HuffCode)) {
            return false;
        }

        return ((HuffCode) obj).getSource() == this.getSource();
    }

    @Override
    public int hashCode() {
        return Integer.valueOf(this.getSource()).hashCode();
    }

    @Override
    public String toString() {
        return "HuffCode{s=" + outVal(this.source) + ",sn="
                + this.sourceNumBits + ",sx="
                + Integer.toHexString(this.source) + ",ts=" + this.targetString
                + ",tn=" + this.targetNumBits + ",f=" + this.frequency + "}";
    }
}