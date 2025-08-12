/*
 * Copyright (c) 2023-2025 Leon Linhart,
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
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * This class provides support for Time-based One-Time Passwords (TOTPs) as
 * specified by <a href="https://www.ietf.org/rfc/rfc6238.txt">RFC&nbsp;6238</a>.
 *
 * <p>The TOTP algorithm is an extension of the HOTP algorithm. The counter that
 * is used by the HOTP algorithm to generate an OTP is replaced by time-based
 * value. Similarly, this class provides an abstraction over an {@link HOTPEngine}.
 * The underlying engine can be fully configured through the builder.</p>
 *
 * <p>Instances of {@code TOTPEngine} are safe for use by multiple concurrent
 * threads.</p>
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public final class TOTPEngine {

    /**
     * The default time step that will be used to generate TOTPs unless
     * otherwise configured through {@link Builder#withTimeStep(Duration)}.
     *
     * <p>The default time step is 30 seconds.</p>
     *
     * @since   0.1.0
     */
    public static final Duration DEFAULT_TIME_STEP = Duration.ofSeconds(30);

    /**
     * {@return a new {@link Builder } instance}
     *
     * @since   0.1.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private final HOTPEngine hotpEngine;
    private final Duration step;

    private TOTPEngine(Builder builder) {
        this.hotpEngine = builder.bHOTPEngine.build();
        this.step = builder.step;
    }

    /**
     * Generates a One-Time Password derived from the given parameters.
     *
     * <p>This method is a utility overload for {@link #generate(SecretKey, Instant)}.
     * The given {@code secret} byte array is wrapped into a temporary {@link SecretKey}
     * instance for the duration of the OTP generation. Afterward, this key is
     * disposed. The given {@code secret} array is not modified.</p>
     *
     * @param secret    the secret to use to generate the OTP
     * @param time      the time for which to generate the OTP
     *
     * @return  the OTP for the given parameters
     *
     * @throws InvalidKeyException      if the given {@code secret} cannot be
     *                                  used as key for the engine's MAC
     * @throws NoSuchAlgorithmException if no supported {@link Mac} instance is
     *                                  available for the configured {@link HOTPEngine.Builder#withMacAlgorithm(String)
     *                                  algorithm}
     *
     * @since   0.1.0
     */
    public String generate(byte[] secret, Instant time) throws NoSuchAlgorithmException, InvalidKeyException {
        long factor = time.toEpochMilli() / this.step.toMillis();
        return this.hotpEngine.generate(secret, factor);
    }

    /**
     * Generates a One-Time Password derived from the given parameters.
     *
     * @param secret    the secret to use to generate the OTP
     * @param time      the time for which to generate the OTP
     *
     * @return  the OTP for the given parameters
     *
     * @throws InvalidKeyException      if the given {@code secret} cannot be
     *                                  used as key for the engine's MAC
     * @throws NoSuchAlgorithmException if no supported {@link Mac} instance is
     *                                  available for the configured {@link HOTPEngine.Builder#withMacAlgorithm(String)
     *                                  algorithm}
     *
     * @apiNote Some {@link Mac} implementations may invalidate the key during
     *          initialization. Thus, a single key instance should never be
     *          passed twice to this method.
     *
     * @since   0.1.0
     */
    public String generate(SecretKey secret, Instant time) throws NoSuchAlgorithmException, InvalidKeyException {
        long factor = time.toEpochMilli() / this.step.toMillis();
        return this.hotpEngine.generate(secret, factor);
    }

    /**
     * A builder for a {@link TOTPEngine}.
     *
     * @since   0.1.0
     */
    public static final class Builder {

        private final HOTPEngine.Builder bHOTPEngine = HOTPEngine.builder();

        private Duration step = DEFAULT_TIME_STEP;

        private Builder() {}

        /**
         * {@return a new {@link TOTPEngine}}
         *
         * @since   0.1.0
         */
        public TOTPEngine build() {
            return new TOTPEngine(this);
        }

        /**
         * Configures the underlying {@link HOTPEngine} for the engine.
         *
         * @param action    the action that is used to configure the underlying
         *                  engine
         *
         * @return  this builder instance
         *
         * @since   0.1.0
         */
        public Builder configureHOTPEngine(Consumer<HOTPEngine.Builder> action) {
            action.accept(this.bHOTPEngine);
            return this;
        }

        /**
         * Sets the time step for the engine.
         *
         * <p>By default, the engine will be configured to use a time step of {@link #DEFAULT_TIME_STEP
         * 30&nbsp;seconds}.</p>
         *
         * @param value the time step to be used
         *
         * @return  this builder instance
         *
         * @since   0.1.0
         */
        public Builder withTimeStep(Duration value) {
            this.step = Objects.requireNonNull(value);
            return this;
        }

    }

}