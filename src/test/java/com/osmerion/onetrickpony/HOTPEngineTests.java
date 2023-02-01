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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link HOTPEngine}.
 *
 * @author  Leon Linhart
 */
public final class HOTPEngineTests {

    private static final byte[] SECRET = "12345678901234567890".getBytes(StandardCharsets.UTF_8);

    private static Stream<Arguments> provideTestData() {
        return IntStream.of(6, 7, 8).boxed().flatMap(digits -> Stream.of(
            Arguments.of("84755224", 0, digits, SECRET),
            Arguments.of("94287082", 1, digits, SECRET),
            Arguments.of("37359152", 2, digits, SECRET),
            Arguments.of("26969429", 3, digits, SECRET),
            Arguments.of("40338314", 4, digits, SECRET),
            Arguments.of("68254676", 5, digits, SECRET),
            Arguments.of("18287922", 6, digits, SECRET),
            Arguments.of("82162583", 7, digits, SECRET),
            Arguments.of("73399871", 8, digits, SECRET),
            Arguments.of("45520489", 9, digits, SECRET)
        ));
    }

    /*
     * Test values from RFC 4226.
     *
     * Count    Hexadecimal    Decimal        HOTP
     * 0        4c93cf18       1284755224     755224
     * 1        41397eea       1094287082     287082
     * 2         82fef30        137359152     359152
     * 3        66ef7655       1726969429     969429
     * 4        61c5938a       1640338314     338314
     * 5        33c083d4        868254676     254676
     * 6        7256c032       1918287922     287922
     * 7         4e5b397         82162583     162583
     * 8        2823443f        673399871     399871
     * 9        2679dc69        645520489     520489
     */

    @ParameterizedTest
    @MethodSource("provideTestData")
    public void test(String otp, int counter, int digits, byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
        HOTPEngine engine = HOTPEngine.builder()
            .withCodeDigits(digits)
            .build();

        assertEquals(otp.substring(8 - digits), engine.generate(secret, counter));
    }

}