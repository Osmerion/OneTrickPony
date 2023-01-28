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
 * specified by <a href="https://www.ietf.org/rfc/rfc4226.txt">RFC&nbsp;4226</a>.
 *
 * <p>Instances of {@link HOTPEngine} are safe for use by multiple concurrent
 * threads.</p>
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public final class HOTPEngine {

    /**
     * The MAC algorithm that will be used to generate HOTPs unless otherwise
     * configured through {@link Builder#withMacAlgorithm(String)}.
     *
     * @since   0.1.0
     */
    public static final String DEFAULT_MAC_ALGORITHM = "HmacSHA1";

    /**
     * The minimum supported number of code digits in an OTP.
     *
     * @see #MAX_SUPPORTED_CODE_DIGITS
     *
     * @since   0.1.0
     */
    public static final int MIN_SUPPORTED_CODE_DIGITS = 6;

    /**
     * The maximum supported number of code digits in an OTP.
     *
     * <p>This is <b>not</b> the maximum supported length of an OTP. If an
     * engine is configured to add an additional {@link Builder#withChecksum(boolean)
     * checksum digit}, it does not count towards the limit for code digits.</p>
     *
     * @see #MIN_SUPPORTED_CODE_DIGITS
     *
     * @since   0.1.0
     */
    public static final int MAX_SUPPORTED_CODE_DIGITS = 8;

    /**
     * A constant value to be passed to {@link Builder#withTruncationOffset(int)}
     * if the engine should perform dynamic truncation as specified in
     * RFC&nbsp;4226.
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

    /**
     * Generates a One-Time Password derived from the given parameters.
     *
     * <p>This method is a utility overload for {@link #generate(SecretKey, long)}.
     * The given {@code secret} byte array is wrapped into a temporary {@link SecretKey}
     * instance for the duration of the OTP generation. Afterward, this key is
     * disposed. The given {@code secret} array is not modified.</p>
     *
     * @param secret        the secret to use to generate the OTP
     * @param counter       the counter value for which to generate the OTP
     *
     * @return  the OTP for the given parameters
     *
     * @throws InvalidKeyException      if the given {@code secret} cannot be
     *                                  used as key for the engine's MAC
     * @throws NoSuchAlgorithmException if no supported {@link Mac} instance is
     *                                  available for the configured {@link Builder#withMacAlgorithm(String)
     *                                  algorithm}
     *
     * @since   0.1.0
     */
    public String generate(byte[] secret, long counter) throws InvalidKeyException, NoSuchAlgorithmException {
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

            return this.generate(key, counter);
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    /**
     * Generates a One-Time Password derived from the given parameters.
     *
     * @param secret        the secret to use to generate the OTP
     * @param counter       the counter value for which to generate the OTP
     *
     * @return  the OTP for the given parameters
     *
     * @throws InvalidKeyException      if the given {@code secret} cannot be
     *                                  used as key for the engine's MAC
     * @throws NoSuchAlgorithmException if no supported {@link Mac} instance is
     *                                  available for the configured {@link Builder#withMacAlgorithm(String)
     *                                  algorithm}
     *
     * @apiNote Some {@link Mac} implementations may invalidate the key during
     *          initialization. Thus, a single key instance should never be
     *          passed twice to this method.
     *
     * @since   0.1.0
     */
    public String generate(SecretKey secret, long counter) throws InvalidKeyException, NoSuchAlgorithmException {
        int totalNumberOfDigits = this.numberOfCodeDigits;
        if (this.checksum) totalNumberOfDigits++;

        byte[] text = new byte[8];

        for (int i = text.length - 1; i >= 0; i--) {
            text[i] = (byte) (counter & 0xFF);
            counter >>= 8;
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
        private String macAlgorithm = DEFAULT_MAC_ALGORITHM;
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
         * Sets whether the engine should add a checksum digit to the OTPs.
         *
         * <p>By default, the engine will be configured not to a checksum
         * digit.</p>
         *
         * @param value whether to add a checksum digit to the OTPs
         *
         * @return  this builder instance
         *
         * @since   0.1.0
         */
        public Builder withChecksum(boolean value) {
            this.checksum = value;
            return this;
        }

        /**
         * Sets the amount of code digits for the engine.
         *
         * <p>By default, the engine will be configured to generate {@code 6}
         * code digits.</p>
         *
         * @param amount    the amount of code digits for an OTP
         *
         * @return  this builder instance
         *
         * @throws IllegalArgumentException if the given {@code amount} is less
         *                                  than the {@link #MIN_SUPPORTED_CODE_DIGITS
         *                                  minimum}, or larger than the {@link #MAX_SUPPORTED_CODE_DIGITS
         *                                  maximum amount of support code
         *                                  digits}
         *
         * @see #MAX_SUPPORTED_CODE_DIGITS
         * @see #MIN_SUPPORTED_CODE_DIGITS
         *
         * @since   0.1.0
         */
        public Builder withCodeDigits(int amount) {
            if (amount < MIN_SUPPORTED_CODE_DIGITS || MAX_SUPPORTED_CODE_DIGITS < amount) {
                throw new IllegalArgumentException("The number of code digits must be in range [MIN_SUPPORTED_CODE_DIGITS, MAX_SUPPORTED_CODE_DIGITS]");
            }

            this.numberOfCodeDigits = amount;
            return this;
        }

        /**
         * Sets the MAC algorithm for the engine.
         *
         * <p>By default, the engine will be configured to use the {@link #DEFAULT_MAC_ALGORITHM}.</p>
         *
         * @param value the mac algorithm to be used
         *
         * @return  this builder instance
         *
         * @apiNote This function does not validate that the given {@code value}
         *          identifies a supported MAC algorithm. Instead, the value is
         *          used to retrieve a {@link Mac} instance as needed when
         *          generating an OTP.
         *
         * @since   0.1.0
         */
        public Builder withMacAlgorithm(String value) {
            this.macAlgorithm = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the truncation offset for the engine.
         *
         * <p>By default, the engine will be configured to perform {@link #USE_DYNAMIC_TRUNCATION
         * dynamic truncation}.</p>
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