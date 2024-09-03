/*
 * Copyright (c) 2023-2024 Leon Linhart,
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.osmerion.onetrickpony;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * The class consists exclusively of static methods for obtaining encoders and
 * decoders for the Base32 encoding scheme. The implementation of this class
 * supports the following types of Base32 as specified in
 * <a href="https://www.ietf.org/rfc/rfc4648.txt">RFC&nbsp;4648</a>.
 *
 * <ul>
 * <li><a id="basic"><b>Basic</b></a>
 * <p> Uses "The Base 32 Alphabet" as specified in Table 3 of RFC&nbsp;4648 for
 *     encoding and decoding operation. The decoder rejects data that contains
 *     characters outside the base32 alphabet.</p></li>
 * <li><a id="hex"><b>Extended Hex Alphabet</b></a>
 * <p> Uses "The 'Extended Hex' Base 32 Alphabet" as specified in Table 4 of
 *     RFC&nbsp;4648 for encoding and decoding operation. The decoder rejects
 *     data that contains characters outside the base32 alphabet.</p></li>
 * </ul>
 *
 * @since   0.1.0
 *
 * @apiNote The API of this class is designed to mirror the API of {@link java.util.Base64}
 *          to provide a consistent experience with the standard library.
 *          However, this class does not provide support for working with
 *          streams since the current implementation does not perform well for
 *          large amounts of data. Similarly, no {@code String} overloads are
 *          provided to avoid issues caused by unexpected charsets.
 *
 * @author  Leon Linhart
 */
public final class Base32 {

    /**
     * {@return a {@link Decoder} that decodes using the <a href="#basic">Basic</a>
     * type base32 encoding scheme}
     *
     * @since   0.1.0
     */
    public static Decoder getDecoder() {
        return Decoder.RFC4648;
    }

    /**
     * {@return a {@link Decoder} that decodes using the <a href="#hex">Extended Hex Alphabet</a>
     * type base32 encoding scheme}
     *
     * @since   0.1.0
     */
    public static Decoder getHexDecoder() {
        return Decoder.RFC4648_HEX;
    }

    /**
     * {@return a {@link Encoder} that encodes using the <a href="#basic">Basic</a>
     * type base32 encoding scheme}
     *
     * @since   0.1.0
     */
    public static Encoder getEncoder() {
        return Encoder.RFC4648;
    }

    /**
     * {@return a {@link Encoder} that encodes using the <a href="#hex">Extended Hex Alphabet</a>
     * type base32 encoding scheme}
     *
     * @since   0.1.0
     */
    public static Encoder getHexEncoder() {
        return Encoder.RFC4648_HEX;
    }

    @Deprecated
    private Base32() {
        throw new UnsupportedOperationException();
    }

    /**
     * This class implements a decoder for decoding byte data using the Base32
     * encoding scheme as specified in RFC&nbsp;4648.
     *
     * <p>Instances of {@code Base32.Decoder} are safe for use by multiple
     * concurrent threads.</p>
     *
     * @since   0.1.0
     */
    public static final class Decoder {

        private static final Decoder RFC4648 = new Decoder(false);
        private static final Decoder RFC4648_HEX = new Decoder(true);

        private static final int[] fromBase32 = new int[256];
        private static final int[] fromBase32Hex = new int[256];

        static {
            Arrays.fill(fromBase32, -1);
            for (int i = 0; i < Encoder.toBase32.length; i++) fromBase32[Encoder.toBase32[i]] = i;
            fromBase32['='] = -2;

            Arrays.fill(fromBase32Hex, -1);
            for (int i = 0; i < Encoder.toBase32Hex.length; i++) fromBase32Hex[Encoder.toBase32Hex[i]] = i;
            fromBase32Hex['='] = -2;
        }

        private final boolean isHex;

        private Decoder(boolean isHex) {
            this.isHex = isHex;
        }

        private static int decodedOutLength(byte[] src, int srcOffset, int srcLimit) {
            int paddings = 0;
            int length = srcLimit - srcOffset;

            if (length == 0) return 0;

            int n = 1;

            while (n <= 7 && srcOffset <= (srcLimit - n)) {
                if (src[srcLimit - n] == '=') {
                    paddings++;
                }

                n++;
            }

            switch (paddings) {
                case 3: {
                    paddings = 2;
                } break;
                case 4: {
                    paddings = 3;
                } break;
                case 6: {
                    paddings = 4;
                } break;
            }

            return 5 * (int) ((length + 7L) / 8) - paddings;
        }

        /**
         * Decodes all bytes from the given input byte array using the
         * {@link Base32} encoding scheme. The results are written into a newly
         * allocated array.
         *
         * @param src   the input to decode
         *
         * @return  a new array containing the decoded bytes
         *
         * @throws IllegalArgumentException if {@code src} contains invalid
         *                                  Base32 data
         *
         * @since   0.1.0
         */
        public byte[] decode(byte[] src) {
            byte[] dst = new byte[decodedOutLength(src, 0, src.length)];
            int ret = this.decode(src, 0, src.length, dst);
            return (ret != dst.length) ? Arrays.copyOf(dst, ret) : dst;
        }

        /**
         * Decodes all bytes from the given input byte array using the
         * {@link Base32} encoding scheme. The results are written into the
         * given output byte array, starting at offset {@code 0}.
         *
         * @param src   the input to decode
         * @param dst   the destination to store the decoded data in
         *
         * @return  the number of bytes written to the output
         *
         * @throws IllegalArgumentException if {@code src} contains invalid
         *                                  Base32 data, or {@code dst} does not
         *                                  have enough space to store the
         *                                  decoded data
         *
         * @since   0.1.0
         */
        public int decode(byte[] src, byte[] dst) {
            int length = decodedOutLength(src, 0, src.length);
            if (dst.length < length || length == -1) {
                throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
            }

            return this.decode(src, 0, src.length, dst);
        }

        /**
         * Decodes all bytes from the input byte buffer using the {@link Base32}
         * encoding scheme. The results are written into a newly allocated
         * buffer.
         *
         * <p>Upon return, the source buffer's position will be updated to its
         * limit; its limit will not have been changed. The returned output
         * buffer's position will be zero and its limit will be the number of
         * resulting decoded bytes.</p>
         *
         * @param buffer    the input to decode
         *
         * @return  a new buffer containing the decoded bytes
         *
         * @throws IllegalArgumentException if {@code buffer} contains invalid
         *                                  Base32 data
         *
         * @since   0.1.0
         */
        public ByteBuffer decode(ByteBuffer buffer) {
            int offset = buffer.position();

            byte[] src;
            int srcOffset, srcLimit;

            if (buffer.hasArray()) {
                src = buffer.array();
                srcOffset = buffer.arrayOffset() + buffer.position();
                srcLimit = buffer.arrayOffset() + buffer.limit();

                buffer.position(buffer.limit());
            } else {
                src = new byte[buffer.remaining()];
                buffer.get(src);

                srcOffset = 0;
                srcLimit = src.length;
            }

            try {
                byte[] dst = new byte[decodedOutLength(src, srcOffset, srcLimit)];
                return ByteBuffer.wrap(dst, 0, this.decode(src, srcOffset, srcLimit, dst));
            } catch (IllegalArgumentException e) {
                buffer.position(offset);
                throw e;
            }
        }

        private int decodeBlock(byte[] src, int srcOffset, int srcLength, byte[] dest, int destOffset) {
            int[] alphabet = this.isHex ? fromBase32Hex : fromBase32;
            int blockLimit = srcOffset + ((srcLength - srcOffset) / 8) * 8;
            int newDestOffset = destOffset;

            while (srcOffset < blockLimit) {
                int b1 = alphabet[src[srcOffset++] & 0xFF];
                int b2 = alphabet[src[srcOffset++] & 0xFF];
                int b3 = alphabet[src[srcOffset++] & 0xFF];
                int b4 = alphabet[src[srcOffset++] & 0xFF];
                int b5 = alphabet[src[srcOffset++] & 0xFF];
                int b6 = alphabet[src[srcOffset++] & 0xFF];
                int b7 = alphabet[src[srcOffset++] & 0xFF];
                int b8 = alphabet[src[srcOffset++] & 0xFF];

                if ((b1 | b2 | b3 | b4 | b5 | b6 | b7 | b8) < 0) { // non base32 byte
                    return newDestOffset - destOffset;
                }

                long bits = (long) b1 << 35 | (long) b2 << 30 | (long) b3 << 25 | (long) b4 << 20
                    | (long) b5 << 15 | (long) b6 << 10 | (long) b7 << 5 | b8;

                dest[newDestOffset++] = (byte) (bits >> 32);
                dest[newDestOffset++] = (byte) (bits >> 24);
                dest[newDestOffset++] = (byte) (bits >> 16);
                dest[newDestOffset++] = (byte) (bits >> 8);
                dest[newDestOffset++] = (byte) (bits);
            }

            return newDestOffset - destOffset;
        }

        private int decode(byte[] src, int offset, int end, byte[] dst) {
            int[] alphabet = this.isHex ? fromBase32Hex : fromBase32;

            int dstOffset = 0;
            long bits = 0;
            int shiftTo = 35;

            while (offset < end) {
                if (shiftTo == 35 && offset < end - 8) {
                    int dstLength = this.decodeBlock(src, offset, end, dst, dstOffset);
                    int charsDecoded = (dstLength / 5) * 8;

                    offset += charsDecoded;
                    dstOffset += dstLength;
                }

                if (offset >= end) {
                    break;
                }

                int b = src[offset++] & 0xFF;
                if ((b = alphabet[b]) < 0) {
                    if (b == -2) { // padding byte '='
                        if (shiftTo == 35 || shiftTo == 30 || shiftTo == 5 ||
                            shiftTo == 25 && (offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=') ||
                            shiftTo == 20 && (offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=') ||
                            shiftTo == 15 && (offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=') ||
                            shiftTo == 10 && (offset == end || src[offset++] != '=' || offset == end || src[offset++] != '=')
                        ) {
                            throw new IllegalArgumentException("Input byte array has wrong 5-byte ending unit");
                        }

                        break;
                    } else {
                        throw new IllegalArgumentException("Illegal base32 character " + Integer.toString(src[offset - 1], 16));
                    }
                }

                bits |= ((long) b << shiftTo);
                shiftTo -= 5;

                if (shiftTo < 0) {
                    dst[dstOffset++] = (byte) (bits >> 32);
                    dst[dstOffset++] = (byte) (bits >> 24);
                    dst[dstOffset++] = (byte) (bits >> 16);
                    dst[dstOffset++] = (byte) (bits >> 8);
                    dst[dstOffset++] = (byte) (bits);
                    shiftTo = 35;
                    bits = 0;
                }
            }

            if (shiftTo == 0 || shiftTo == 5) {
                dst[dstOffset++] = (byte)(bits >> 32);
                dst[dstOffset++] = (byte)(bits >> 24);
                dst[dstOffset++] = (byte)(bits >> 16);
                dst[dstOffset++] = (byte)(bits >> 8);
            } else if (shiftTo == 10) {
                dst[dstOffset++] = (byte) (bits >> 32);
                dst[dstOffset++] = (byte) (bits >> 24);
                dst[dstOffset++] = (byte) (bits >> 16);
            } else if (shiftTo == 15) {
                dst[dstOffset++] = (byte) (bits >> 32);
                dst[dstOffset++] = (byte) (bits >> 24);
            } else if (shiftTo == 20 | shiftTo == 25) {
                dst[dstOffset++] = (byte) (bits >> 32);
            } else if (shiftTo == 30) {
                throw new IllegalArgumentException("Last unit does not have enough valid bits");
            }

            if (offset < end) {
                throw new IllegalArgumentException("Input byte array has incorrect ending byte at " + offset);
            }

            return dstOffset;
        }

    }

    /**
     * This class implements an encoder for encoding byte data using the Base32
     * encoding scheme as specified in RFC&nbsp;4648.
     *
     * <p>Instances of {@code Base32.Encoder} are safe for use by multiple
     * concurrent threads.</p>
     *
     * @since   0.1.0
     */
    public static final class Encoder {

        private static final Encoder RFC4648 = new Encoder(false);
        private static final Encoder RFC4648_HEX = new Encoder(true);

        private static final char[] toBase32 = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '2', '3', '4', '5', '6', '7'
        };

        private static final char[] toBase32Hex = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
            'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V'
        };

        private static int encodedOutLength(int srcLength, boolean throwOOME) {
            try {
                return Math.multiplyExact(8, Math.addExact(srcLength, 4) / 5);
            } catch (ArithmeticException e) {
                if (throwOOME) {
                    throw new OutOfMemoryError("Encoded size is too large");
                } else {
                    return -1;
                }
            }
        }

        private final boolean isHex;

        private Encoder(boolean isHex) {
            this.isHex = isHex;
        }

        /**
         * Encodes all bytes from the given input byte array using the
         * {@link Base32} encoding scheme. The results are written into a newly
         * allocated array.
         *
         * @param src   the input to encode
         *
         * @return  a new array containing the encoded bytes
         *
         * @since   0.1.0
         */
        public byte[] encode(byte[] src) {
            int length = encodedOutLength(src.length, true);
            byte[] dst = new byte[length];
            int ret = this.encode(src, 0, src.length, dst);
            return (ret != dst.length) ? Arrays.copyOf(dst, ret) : dst;
        }

        /**
         * Encodes all bytes from the given input byte array using the
         * {@link Base32} encoding scheme. The results are written into the
         * given output byte array, starting at offset {@code 0}.
         *
         * @param src   the input to encode
         * @param dst  the destination to store the encoded data in
         *
         * @return  the number of bytes written to the output
         *
         * @throws IllegalArgumentException if {@code dst} does not have enough
         *                                  space to store the encoded data
         *
         * @since   0.1.0
         */
        public int encode(byte[] src, byte[] dst) {
            int length = encodedOutLength(src.length, false);
            if (dst.length < length || length == -1) {
                throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
            }

            return this.encode(src, 0, src.length, dst);
        }

        /**
         * Encodes all bytes from the input byte buffer using the {@link Base32}
         * encoding scheme. The results are written into a newly allocated
         * buffer.
         *
         * <p>Upon return, the source buffer's position will be updated to its
         * limit; its limit will not have been changed. The returned output
         * buffer's position will be zero and its limit will be the number of
         * resulting encoded bytes.</p>
         *
         * @param buffer    the input to encode
         *
         * @return  a new buffer containing the encoded bytes
         *
         * @since   0.1.0
         */
        public ByteBuffer encode(ByteBuffer buffer) {
            int length = encodedOutLength(buffer.remaining(), true);
            byte[] dst = new byte[length];
            int ret;

            if (buffer.hasArray()) {
                ret = this.encode(
                    buffer.array(),
                    buffer.arrayOffset() + buffer.position(),
                    buffer.arrayOffset() + buffer.limit(),
                    dst
                );

                buffer.position(buffer.limit());
            } else {
                byte[] src = new byte[buffer.remaining()];
                buffer.get(src);

                ret = this.encode(src, 0, src.length, dst);
            }

            return ByteBuffer.wrap((ret != dst.length) ? Arrays.copyOf(dst, ret) : dst);
        }

        private void encodeBlock(byte[] src, int srcOffset, int srcLimit, byte[] dst, int dstOffset) {
            char[] alphabet = this.isHex ? toBase32Hex : toBase32;

            while (srcOffset < srcLimit) {
                long bits = (long) (src[srcOffset++] & 0xFF) << 32 |
                    (long) (src[srcOffset++] & 0xFF) << 24 |
                    (long) (src[srcOffset++] & 0xFF) << 16 |
                    (long) (src[srcOffset++] & 0xFF) << 8 |
                    (long) (src[srcOffset++] & 0xFF);

                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 35) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 30) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 25) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 20) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 15) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 10) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) ((bits >>> 5) & 0b11111)];
                dst[dstOffset++] = (byte) alphabet[(int) (bits & 0b11111)];
            }
        }

        private int encode(byte[] src, int srcOffset, int srcLimit, byte[] dst) {
            char[] alphabet = this.isHex ? toBase32Hex : toBase32;
            int dstOffset = 0;

            int slen = ((srcLimit - srcOffset) / 5) * 5;
            int sl = srcOffset + slen;

            while (srcOffset < sl) {
                int sl0 = Math.min(srcOffset + slen, sl);

                this.encodeBlock(src, srcOffset, sl0, dst, dstOffset);
                dstOffset += (sl0 - srcOffset) / 5 * 8;
                srcOffset = sl0;
            }

            if (srcOffset < srcLimit) { // 1-4 leftover bytes
                int b0 = src[srcOffset++] & 0xFF;
                dst[dstOffset++] = (byte) alphabet[b0 >> 3];

                if (srcOffset == srcLimit) {
                    dst[dstOffset++] = (byte) alphabet[(b0 << 2) & 0b11111];
                    dst[dstOffset++] = '=';
                    dst[dstOffset++] = '=';
                    dst[dstOffset++] = '=';
                    dst[dstOffset++] = '=';
                    dst[dstOffset++] = '=';
                } else {
                    int b1 = src[srcOffset++] & 0xFF;
                    dst[dstOffset++] = (byte) alphabet[(b0 << 2) & 0b11111 | (b1 >> 6)];
                    dst[dstOffset++] = (byte) alphabet[(b1 >> 1) & 0b11111];

                    if (srcOffset == srcLimit) {
                        dst[dstOffset++] = (byte) alphabet[(b1 << 4) & 0b11111];
                        dst[dstOffset++] = '=';
                        dst[dstOffset++] = '=';
                        dst[dstOffset++] = '=';
                    } else {
                        int b2 = src[srcOffset++] & 0xFF;
                        dst[dstOffset++] = (byte) alphabet[(b1 << 4) & 0b11111 | (b2 >> 4)];

                        if (srcOffset == srcLimit) {
                            dst[dstOffset++] = (byte) alphabet[(b2 << 1) & 0b11111];
                            dst[dstOffset++] = '=';
                            dst[dstOffset++] = '=';
                        } else {
                            int b3 = src[srcOffset] & 0xFF;
                            dst[dstOffset++] = (byte) alphabet[(b2 << 1) & 0b11111 | (b3 >> 7)];
                            dst[dstOffset++] = (byte) alphabet[(b3 >> 2) & 0b11111];
                            dst[dstOffset++] = (byte) alphabet[(b3 << 3) & 0b11111];
                        }
                    }

                }

                dst[dstOffset++] = '=';
            }

            return dstOffset;
        }

    }

}