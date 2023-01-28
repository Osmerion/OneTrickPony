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

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This class provides support for Time-based One-Time Passwords (TOTPs) as
 * specified by <a href="https://www.ietf.org/rfc/rfc6238.txt">RFC 6238</a>.
 *
 * @since   0.1.0
 *
 * @author  Leon Linhart
 */
public final class TOTPEngine {

    /**
     * {@return a new {@link Builder } instance}
     *
     * @since   0.1.0
     */
    public static Builder builder() {
        return new Builder();
    }

    private final HOTPEngine hotpEngine;
    private final int stepSeconds;

    private TOTPEngine(Builder builder) {
        this.hotpEngine = builder.bHOTPEngine.build();
        this.stepSeconds = builder.stepSeconds;
    }

    public void generate(byte[] secret, long timeMillis) throws NoSuchAlgorithmException, InvalidKeyException {
        long factor = timeMillis / (1000L * this.stepSeconds);
        this.hotpEngine.generate(secret, factor);
    }

    /**
     * A builder for a {@link TOTPEngine}.
     *
     * @since   0.1.0
     */
    public static final class Builder {

        private final HOTPEngine.Builder bHOTPEngine = HOTPEngine.builder();

        private int stepSeconds = 0;

        private Builder() {}

        /**
         * {@return a new {@link TOTPEngine}}
         *
         * @since   0.1.0
         */
        public TOTPEngine build() {
            return new TOTPEngine(this);
        }

        public Builder withTimeStep(int seconds) {
            this.stepSeconds = seconds;
            return this;
        }

    }

}