# OneTrickPony 

[![License](https://img.shields.io/badge/license-BSD-blue.svg?style=for-the-badge&label=License)](https://github.com/Osmerion/OneTrickPony/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.osmerion.onetrickpony/onetrickpony.svg?style=for-the-badge&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/com.osmerion.onetrickpony/onetrickpony)
[![JavaDoc](https://img.shields.io/maven-central/v/com.osmerion.onetrickpony/onetrickpony.svg?style=for-the-badge&label=JavaDoc&color=blue)](https://javadoc.io/doc/com.osmerion.onetrickpony/onetrickpony)
![Java](https://img.shields.io/badge/Java-11-green.svg?style=for-the-badge&color=b07219&logo=java)

OneTrickPony is a modern Java library that implements support for One-Time
Passwords (OTPs). The library requires Java 11 or later and is fully compatible
with Java's module system. It has zero runtime dependencies on external
libraries. Built-In support is provided for the HOTP ([RFC&nbsp;4226](https://www.rfc-editor.org/rfc/rfc4226))
and TOTP ([RFC&nbsp;6238](https://www.rfc-editor.org/rfc/rfc6238)) algorithms.


## Getting Started

OneTrickPony provides support for OTPs via so-called _engines_.
The following engines are provided by the library.

### HMAC-based One-Time Passwords (HOTPs)

The `HOTPEngine` provides support for HOTPs as specified by RFC&nbsp;4226.

```java
HOTPEngine engine = HOTPEngine.builder()
    .withChecksum(false)
    .withCodeDigits(6)
    .withMacAlgorithm("HmacSHA1")
    .withTruncationOffset(HOTPEngine.USE_DYNAMIC_TRUNCATION)
    .build();

byte[] secret = "12345678901234567890".getBytes(StandardCharsets.UTF_8);
int counter = 0;

String otp = engine.generate(secret, counter);
System.out.println(otp); // 755224
```

The engine's properties can be configured through a builder. All properties are
initialized with reasonable defaults that are sufficient for most use-cases.

| Property          |                                                                                            | Default                  |
|-------------------|--------------------------------------------------------------------------------------------|--------------------------|
| Checksum          | Whether to add an additional checksum digit to the OTP                                     | `false`                  |
| Code digits       | The number of code digits of the OTP                                                       | `6`                      |
| MAC algorithm     | The MAC algorithm to use to generate the hash for the OTP                                  | `HmacSHA1`               |
| Truncation offset | The offset that will be used to extract the bytes used for the OTP from the generated hash | `USE_DYNAMIC_TRUNCATION` |

The MAC algorithm is used to retrieve a [javax.crypto.Mac](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/javax/crypto/Mac.html)
instance. Check the documentation of your Java distribution for a list of
supported algorithms.


### Time-based One-Time Passwords (TOTPs)

The `TOTPEngine` provides support for TOTPs as specified by RFC&nbsp;6238.

```java
TOTPEngine engine = TOTPEngine.builder()
    .configureHOTPEngine(hotpBuilder -> {
        // Use the builder to configure the underlying HOTPEngine
    })
    .withTimeStep(Duration.ofSeconds(30))
    .build();

byte[] secret = "12345678901234567890".getBytes(StandardCharsets.UTF_8);
Instant time = Instant.parse("1970-01-01T00:00:59.00Z");

String otp = engine.generate(secret, time);
System.out.println(otp); // 287082
```

This engine uses an underlying `HOTPEngine` to generate the OTP. Both, the
engine itself and the underlying engine can be configured through a builder.

The engine has a single additional property to configure the time step for the
generated OTPs. It defaults to a time step of 30&nbsp;seconds.


### Bonus: The Base32 Encoding Scheme

As secrets for OTPs are commonly shared using the Base32 encoding scheme,
OneTrickPony also provides a `Base32` class analogous to Java's
`java.util.Base64` class. This class implements support for Base32 en- and
decoding as specified by [RFC&nbsp;6238](https://www.rfc-editor.org/rfc/rfc4648).

```java
// Retrieve reusable en- and decoder instances
Base32.Encoder encoder = Base32.getEncoder();
Base32.Decoder decoder = Base32.getDecoder();

// Encoding
byte[] base32Data = encoder.encode("foobar".getBytes(StandardCharset.UTF_8));
System.out.println(new String(base32Data, StandardCharsets.UTF_8)); // MZXW6YTBOI======

// Decoding
byte[] decodedData = decoder.decode(base32Data);
System.out.println(new String(decodedData, StandardCharsets.UTF_8)); // foobar
```


## Building from source

### Setup

This project uses [Gradle's toolchain support](https://docs.gradle.org/current/userguide/toolchains.html)
to detect and select the JDKs required to run the build. Please refer to the
build scripts to find out which toolchains are requested.

An installed JDK 17 (or later) is required to use Gradle.

### Building

Once the setup is complete, invoke the respective Gradle tasks using the
following command on Unix/macOS:

    ./gradlew <tasks>

or the following command on Windows:

    gradlew <tasks>

Important Gradle tasks to remember are:
- `clean`                   - clean build results
- `build`                   - assemble and test the Java library
- `publishToMavenLocal`     - build and install all public artifacts to the
                              local maven repository

Additionally `tasks` may be used to print a list of all available tasks.


## License

OneTrickPony is available under the terms of the [3-Clause BSD license](https://spdx.org/licenses/BSD-3-Clause.html).

```
Copyright (c) 2023-2024 Leon Linhart,
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
```