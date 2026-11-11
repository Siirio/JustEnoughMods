# Third-party notices

## Gradle wrapper

`gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` were copied from the official MinecraftForge 47.4.10 reference checkout supplied for this research. They are standard Gradle wrapper components. Their copyright headers are retained. Gradle wrapper software is distributed under the Apache License, Version 2.0; the license is included at `licenses/Gradle-Apache-2.0.txt`.

The wrapper selects the official Gradle 8.8 binary distribution. Its SHA-256 was retrieved from:

<https://services.gradle.org/distributions/gradle-8.8-bin.zip.sha256>

Digest: `a4b4158601f8636cdeeab09bd76afb640030bb5b144aafe261a5e8af027dc612`.

The copied wrapper JAR SHA-256 is `ed2c26eba7cfb93cc2b7785d05e534f07b5b48b5e7fc941921cd098628abca58`. The wrapper configuration adds the distribution digest; the launcher scripts and wrapper JAR are otherwise unmodified.

## Runtime dependencies

Forge, Minecraft, Gson, LWJGL, and other platform libraries are resolved by the normal Forge development/runtime environment under their respective licenses. Their implementation sources and binaries are not vendored into this module, apart from the explicitly attributed Gradle wrapper. Taste Engine's configuration, UI and renderer implementation are original.

## Inspiration research

Luxium and Shine have proprietary licenses. Immersive VFX has its own licensing terms documented in the handoff research. This module does not contain their source code, shaders, textures, presets, or decompiled classes. Conceptual analysis does not grant permission to copy those implementations.
