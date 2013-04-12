package org.sunnycode.huff;

public class HuffmanEncodingException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public HuffmanEncodingException() {
        super();
    }

    public HuffmanEncodingException(String message) {
        super(message);
    }
}
