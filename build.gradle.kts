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
plugins {
    alias(libs.plugins.extra.java.module.info)
    alias(libs.plugins.gradle.toolchain.switches)
    id("com.osmerion.maven-publish-conventions")
    `java-library`
}

val artifactName = "onetrickpony"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(19)
    }

    withJavadocJar()
    withSourcesJar()
}

tasks {
    withType<JavaCompile>().configureEach {
        options.javaModuleVersion = "${project.version}"
        options.release = 11
    }

    withType<Jar>().configureEach {
        archiveBaseName = artifactName
    }

    javadoc {
        with(options as StandardJavadocDocletOptions) {
            tags = listOf(
                "apiNote:a:API Note:",
                "implSpec:a:Implementation Requirements:",
                "implNote:a:Implementation Note:"
            )

            addStringOption("-release", "11")
        }
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = artifactName

            pom {
                description =
                    """
                    OneTrickPony is a modern Java library that implements support for One-Time
                    Passwords (OTPs). The library requires Java 11 or later and is fully compatible
                    with Java's module system. It has zero runtime dependencies on external
                    libraries. Built-In support is provided for the HOTP (RFC 4226) and
                    TOTP (RFC 6238) algorithms.
                    """.trimIndent().lines().joinToString(separator = " ")
            }
        }
    }
}

extraJavaModuleInfo {
    automaticModule(libs.jsr305.get().module.toString(), "jsr305")
}

dependencies {
    compileOnly(libs.jsr305)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
}