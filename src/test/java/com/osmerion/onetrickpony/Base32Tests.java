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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Base32}.
 *
 * @author  Leon Linhart
 */
public final class Base32Tests {

    private static Stream<Arguments> toArguments(StringPair... data) {
        return Stream.of(data).map(it -> Arguments.of(
            it.source().getBytes(StandardCharsets.UTF_8),
            it.expectedResult().getBytes(StandardCharsets.UTF_8)
        ));
    }

    private static Stream<Arguments> provideData() {
        return toArguments(
            new StringPair("", ""),
            new StringPair("f", "MY======"),
            new StringPair("fo", "MZXQ===="),
            new StringPair("foo", "MZXW6==="),
            new StringPair("foob", "MZXW6YQ="),
            new StringPair("fooba", "MZXW6YTB"),
            new StringPair("foobar", "MZXW6YTBOI======"),

            new StringPair(" ", "EA======"),
            new StringPair("  ", "EAQA===="),
            new StringPair("   ", "EAQCA==="),
            new StringPair("    ", "EAQCAIA="),
            new StringPair("     ", "EAQCAIBA"),
            new StringPair("      ", "EAQCAIBAEA======")
        );
    }

    private static Stream<Arguments> provideHexData() {
        return toArguments(
            new StringPair("", ""),
            new StringPair("f", "CO======"),
            new StringPair("fo", "CPNG===="),
            new StringPair("foo", "CPNMU==="),
            new StringPair("foob", "CPNMUOG="),
            new StringPair("fooba", "CPNMUOJ1"),
            new StringPair("foobar", "CPNMUOJ1E8======")
        );
    }

    private static Stream<Arguments> provideRandomData() {
        Random random = new Random(System.currentTimeMillis());

        return IntStream.range(0, 1_000).boxed().flatMap(it -> {
            byte[] data = new byte[64 + random.nextInt(64)];
            random.nextBytes(data);

            return Stream.of(
                Arguments.of(Base32.getDecoder(), Base32.getEncoder(), data),
                Arguments.of(Base32.getHexDecoder(), Base32.getHexEncoder(), data)
            );
        });
    }

    @ParameterizedTest
    @MethodSource("provideRandomData")
    public void entropyTest(Base32.Decoder decoder, Base32.Encoder encoder, byte[] data) {
        byte[] encodedData = assertDoesNotThrow(() -> encoder.encode(data));
        assertArrayEquals(data, decoder.decode(encodedData));
    }

    @ParameterizedTest
    @MethodSource("provideData")
    public void testDecoder(byte[] src, byte[] encoded) {
        Base32.Decoder decoder = Base32.getDecoder();
        assertArrayEquals(src, decoder.decode(encoded));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5, 6, 7, 8 })
    public void testDecodeEnding(int trim) {
        Base32.Decoder decoder = Base32.getDecoder();
        byte[] data = "ITB3M3TFOJRHKZDF".getBytes(StandardCharsets.UTF_8); // Dönerbude

        if (trim == 7) {
            assertThrows(IllegalArgumentException.class, () -> decoder.decode(Arrays.copyOf(data, data.length - trim)));
        } else {
            assertDoesNotThrow(() -> decoder.decode(Arrays.copyOf(data, data.length - trim)));
        }
    }

    @Test
    public void testDecodeInvalidCharacter() {
        Base32.Decoder decoder = Base32.getDecoder();
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("-".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @MethodSource("provideHexData")
    public void testHexDecoder(byte[] src, byte[] encoded) {
        Base32.Decoder decoder = Base32.getHexDecoder();
        assertArrayEquals(src, decoder.decode(encoded));
    }

    @Test
    public void testHexDecodeInvalidCharacter() {
        Base32.Decoder decoder = Base32.getHexDecoder();
        assertThrows(IllegalArgumentException.class, () -> decoder.decode("-".getBytes(StandardCharsets.UTF_8)));
    }

    @ParameterizedTest
    @MethodSource("provideData")
    public void testEncoder(byte[] src, byte[] encoded) {
        Base32.Encoder encoder = Base32.getEncoder();
        assertArrayEquals(encoded, encoder.encode(src));
    }

    @ParameterizedTest
    @MethodSource("provideHexData")
    public void testHexEncoder(byte[] src, byte[] encoded) {
        Base32.Encoder encoder = Base32.getHexEncoder();
        assertArrayEquals(encoded, encoder.encode(src));
    }

}