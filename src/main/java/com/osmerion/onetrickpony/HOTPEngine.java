/*
 * Copyright (c) 2023 Leon Linhart,
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

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class provides support for HMAC-based One-Time Passwords (HOTPs) as
 * specified by <a href="https://www.ietf.org/rfc/rfc4226.txt">RFC 4226</a>.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public final class HOTPEngine {

    /**
     * TODO doc
     *
     * @since   0.1.0
     */
    public static final int MAX_SUPPORTED_CODE_DIGITS = 8;

    /**
     * A constant value to be passed to {@link Builder#withTruncationOffset(int)}
     * if the engine should perform dynamic truncation as specified in RFC 4226.
     *
     * @since   0.1.0
     */
    public static final int USE_DYNAMIC_TRUNCATION = -1;

    /**
     * {@return a new {@link Builder} instance}
     *
     * @since   0.1.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private static final int[] DOUBLE_DIGITS =
//        0  1  2  3  4  5  6  7  8  9
        { 0, 2, 4, 6, 8, 1, 3, 5, 7, 9 };

    private static int computeChecksumDigit(long data, int digits) {
        boolean doubleDigit = true;
        int total = 0;

        while (0 < digits--) {
            int digit = (int) (data % 10);
            data /= 10;

            if (doubleDigit) {
                digit = DOUBLE_DIGITS[digit];
            }

            total += digit;
            doubleDigit = !doubleDigit;
        }

        int result = total % 10;
        if (result > 0) result = 10 - result;

        return result;
    }

    private static final ThreadLocal<Map<String, Mac>> THREAD_LOCAL_MAC = ThreadLocal.withInitial(() -> new HashMap<>(1));

    private static Mac getMac(String algorithm) throws NoSuchAlgorithmException {
        Map<String, Mac> cachedMacs = THREAD_LOCAL_MAC.get();
        Mac mac = cachedMacs.get(algorithm);

        if (mac == null) {
            mac = Mac.getInstance(algorithm);
            cachedMacs.put(algorithm, mac);
        }

        return mac;
    }

    private final boolean checksum;
    private final String macAlgorithm;
    private final int numberOfCodeDigits;
    private final int truncationOffset;

    private HOTPEngine(Builder builder) {
        this.checksum = builder.checksum;
        this.macAlgorithm = builder.macAlgorithm;
        this.numberOfCodeDigits = builder.numberOfCodeDigits;
        this.truncationOffset = builder.truncationOffset;
    }

    public String generate(byte[] secret, long movingFactor) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] bytes = Arrays.copyOf(secret, secret.length);

        try {
            SecretKey key = new SecretKey() {

                @Override
                public String getAlgorithm() {
                    return "RAW";
                }

                @Override
                public String getFormat() {
                    return "RAW";
                }

                @Override
                public byte[] getEncoded() {
                    return bytes;
                }

            };

            return this.generate(key, movingFactor);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * TODO doc
     *
     * @param secret
     * @param movingFactor
     *
     * @return
     *
     * @since   0.1.0
     */
    public String generate(SecretKey secret, long movingFactor) throws InvalidKeyException, NoSuchAlgorithmException {
        int totalNumberOfDigits = this.numberOfCodeDigits;
        if (this.checksum) totalNumberOfDigits++;

        byte[] text = new byte[8];

        for (int i = text.length - 1; i >= 0; i--) {
            text[i] = (byte) (movingFactor & 0xff);
            movingFactor >>= 8;
        }

        Mac mac = getMac(this.macAlgorithm);
        mac.init(secret);

        byte[] hash = mac.doFinal(text);
        int offset = hash[hash.length - 1] & 0xF;

        if ((0 <= this.truncationOffset) && (this.truncationOffset < (hash.length - 4))) {
            offset = this.truncationOffset;
        }

        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);

        int otp = binary % ((int) Math.pow(10, this.numberOfCodeDigits));

        if (this.checksum) {
            otp = (otp * 10) + computeChecksumDigit(otp, this.numberOfCodeDigits);
        }

        StringBuilder result = new StringBuilder(String.valueOf(otp));
        while (result.length() < totalNumberOfDigits) result.insert(0, "0");

        return result.toString();
    }

    /**
     * A builder for an {@link HOTPEngine}.
     *
     * @since   0.1.0
     */
    public static final class Builder {

        private boolean checksum = false;
        private String macAlgorithm = "HmacSHA1";
        private int numberOfCodeDigits = 6;
        private int truncationOffset = USE_DYNAMIC_TRUNCATION;

        private Builder() {}

        /**
         * {@return a new {@link HOTPEngine}}
         *
         * @since   0.1.0
         */
        public HOTPEngine build() {
            return new HOTPEngine(this);
        }

        /**
         * TODO doc
         *
         * @param value
         *
         * @return  this builder instance
         *
         * @since   0.1.0
         */
        public Builder withChecksum(boolean value) {
            this.checksum = value;
            return this;
        }

        public Builder withCodeDigits(int amount) {
            if (amount < 0 || MAX_SUPPORTED_CODE_DIGITS < amount) throw new IllegalArgumentException();

            this.numberOfCodeDigits = amount;
            return this;
        }

        /**
         * TODO doc
         *
         * @param value
         *
         * @return  this builder instance
         *
         * @since   0.1.0
         */
        public Builder withMacAlgorithm(String value) {
            this.macAlgorithm = Objects.requireNonNull(value);
            return this;
        }

        /**
         * TODO doc
         *
         * <p>By default, the engine will be configured to perform dynamic
         * truncation.</p>
         *
         * @param value the desired truncation offset, or {@link #USE_DYNAMIC_TRUNCATION}
         *
         * @return  this builder instance
         *
         * @throws IllegalArgumentException if the given value is not in [-1, 15]
         *
         * @since   0.1.0
         */
        public Builder withTruncationOffset(int value) {
            if (value < USE_DYNAMIC_TRUNCATION || 15 < value) throw new IllegalArgumentException("The truncation offset must be in range [0, 15] or USE_DYNAMIC_TRUNCATION");
            this.truncationOffset = value;
            return this;
        }

    }

}