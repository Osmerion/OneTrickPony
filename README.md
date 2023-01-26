# OneTrickPony 

[![License](https://img.shields.io/badge/license-BSD-blue.svg?style=flat-square&label=License)](https://github.com/Osmerion/OneTrickPony/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.osmerion.onetrickpony/onetrickpony.svg?style=flat-square&label=Maven%20Central)](https://maven-badges.herokuapp.com/maven-central/com.osmerion.onetrickpony/onetrickpony)
[![JavaDoc](https://img.shields.io/maven-central/v/com.osmerion.onetrickpony/onetrickpony.svg?style=flat-square&label=JavaDoc&color=blue)](https://javadoc.io/doc/com.osmerion.onetrickpony/onetrickpony)
![Java](https://img.shields.io/badge/Java-11-green.svg?style=flat-square&color=b07219&logo=java)

OneTrickPony is a modern Java library that implements support for One-Time
Passwords (OTPs). The library requires Java 11 or later and is fully compatible
with Java's module system. It has zero runtime dependencies on external
libraries. Built-In support is provided for the HOTP ([RFC 4226](https://www.rfc-editor.org/rfc/rfc4226))
and TOTP ([RFC 6238](https://www.rfc-editor.org/rfc/rfc6238)) algorithms.

## Getting Started

!TODO


## Building from source

### Setup

This project uses [Gradle's toolchain support](https://docs.gradle.org/7.6/userguide/toolchains.html)
to detect and select the JDKs required to run the build. Please refer to the
build scripts to find out which toolchains are requested.

An installed JDK 1.8 (or later) is required to use Gradle.

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

```
Copyright (c) 2023 Leon Linhart,
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