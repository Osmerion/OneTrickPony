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
import java.time.Instant;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for {@link TOTPEngine}.
 *
 * @author  Leon Linhart
 */
public final class TOTPEngineTests {

    private static final byte[] SECRET_SHA1 = "12345678901234567890".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] SECRET_SHA256 = "12345678901234567890123456789012".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] SECRET_SHA512 = "1234567890123456789012345678901234567890123456789012345678901234".getBytes(StandardCharsets.ISO_8859_1);

    private static Stream<Arguments> provideTestData() {
        return IntStream.of(6, 7, 8).boxed().flatMap(digits -> Stream.of(
            Arguments.of("94287082", "1970-01-01T00:00:59.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("46119246", "1970-01-01T00:00:59.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("90693936", "1970-01-01T00:00:59.00Z", digits, "HmacSHA512", SECRET_SHA512),

            Arguments.of("07081804", "2005-03-18T01:58:29.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("68084774", "2005-03-18T01:58:29.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("25091201", "2005-03-18T01:58:29.00Z", digits, "HmacSHA512", SECRET_SHA512),

            Arguments.of("14050471", "2005-03-18T01:58:31.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("67062674", "2005-03-18T01:58:31.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("99943326", "2005-03-18T01:58:31.00Z", digits, "HmacSHA512", SECRET_SHA512),

            Arguments.of("89005924", "2009-02-13T23:31:30.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("91819424", "2009-02-13T23:31:30.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("93441116", "2009-02-13T23:31:30.00Z", digits, "HmacSHA512", SECRET_SHA512),

            Arguments.of("69279037", "2033-05-18T03:33:20.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("90698825", "2033-05-18T03:33:20.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("38618901", "2033-05-18T03:33:20.00Z", digits, "HmacSHA512", SECRET_SHA512),

            Arguments.of("65353130", "2603-10-11T11:33:20.00Z", digits, "HmacSHA1", SECRET_SHA1),
            Arguments.of("77737706", "2603-10-11T11:33:20.00Z", digits, "HmacSHA256", SECRET_SHA256),
            Arguments.of("47863826", "2603-10-11T11:33:20.00Z", digits, "HmacSHA512", SECRET_SHA512)
        ));
    }

    /*
     * Test values from RFC 6238.
     *
     * +-------------+--------------+------------------+----------+--------+
     * |  Time (sec) |   UTC Time   | Value of T (hex) |   TOTP   |  Mode  |
     * +-------------+--------------+------------------+----------+--------+
     * |      59     |  1970-01-01  | 0000000000000001 | 94287082 |  SHA1  |
     * |             |   00:00:59   |                  |          |        |
     * |      59     |  1970-01-01  | 0000000000000001 | 46119246 | SHA256 |
     * |             |   00:00:59   |                  |          |        |
     * |      59     |  1970-01-01  | 0000000000000001 | 90693936 | SHA512 |
     * |             |   00:00:59   |                  |          |        |
     * |  1111111109 |  2005-03-18  | 00000000023523EC | 07081804 |  SHA1  |
     * |             |   01:58:29   |                  |          |        |
     * |  1111111109 |  2005-03-18  | 00000000023523EC | 68084774 | SHA256 |
     * |             |   01:58:29   |                  |          |        |
     * |  1111111109 |  2005-03-18  | 00000000023523EC | 25091201 | SHA512 |
     * |             |   01:58:29   |                  |          |        |
     * |  1111111111 |  2005-03-18  | 00000000023523ED | 14050471 |  SHA1  |
     * |             |   01:58:31   |                  |          |        |
     * |  1111111111 |  2005-03-18  | 00000000023523ED | 67062674 | SHA256 |
     * |             |   01:58:31   |                  |          |        |
     * |  1111111111 |  2005-03-18  | 00000000023523ED | 99943326 | SHA512 |
     * |             |   01:58:31   |                  |          |        |
     * |  1234567890 |  2009-02-13  | 000000000273EF07 | 89005924 |  SHA1  |
     * |             |   23:31:30   |                  |          |        |
     * |  1234567890 |  2009-02-13  | 000000000273EF07 | 91819424 | SHA256 |
     * |             |   23:31:30   |                  |          |        |
     * |  1234567890 |  2009-02-13  | 000000000273EF07 | 93441116 | SHA512 |
     * |             |   23:31:30   |                  |          |        |
     * |  2000000000 |  2033-05-18  | 0000000003F940AA | 69279037 |  SHA1  |
     * |             |   03:33:20   |                  |          |        |
     * |  2000000000 |  2033-05-18  | 0000000003F940AA | 90698825 | SHA256 |
     * |             |   03:33:20   |                  |          |        |
     * |  2000000000 |  2033-05-18  | 0000000003F940AA | 38618901 | SHA512 |
     * |             |   03:33:20   |                  |          |        |
     * | 20000000000 |  2603-10-11  | 0000000027BC86AA | 65353130 |  SHA1  |
     * |             |   11:33:20   |                  |          |        |
     * | 20000000000 |  2603-10-11  | 0000000027BC86AA | 77737706 | SHA256 |
     * |             |   11:33:20   |                  |          |        |
     * | 20000000000 |  2603-10-11  | 0000000027BC86AA | 47863826 | SHA512 |
     * |             |   11:33:20   |                  |          |        |
     * +-------------+--------------+------------------+----------+--------+
     */

    @ParameterizedTest
    @MethodSource("provideTestData")
    public void test(String otp, String time, int digits, String mac, byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
        TOTPEngine engine = TOTPEngine.builder()
            .configureHOTPEngine(hotp -> {
                hotp.withCodeDigits(digits);
                hotp.withMacAlgorithm(mac);
            })
            .build();

        assertEquals(otp.substring(8 - digits), engine.generate(secret, Instant.parse(time)));
    }

}