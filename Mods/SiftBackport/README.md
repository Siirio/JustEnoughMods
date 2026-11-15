# Sift Backport Java 17 upstream patch

This module owns the disclosed Java 17 rebuild of Sift Backport 1.0.5.1 for the Major Update 4 branch

- Upstream source: `https://github.com/Derec-Mods/Sift-Backport.git`
- Pinned commit: `82a7428b31e29bcebaa127c3118214c9f17421fa`
- Upstream-declared license: MIT in `forge/1.20.1/gradle.properties` and published project metadata
- Upstream does not contain a standalone `LICENSE` file at the pinned commit
- Runtime target: Minecraft 1.20.1, Forge 47 and Java 17

The patch changes only build compatibility, production packaging and disclosure. It changes the Java toolchain from 21 to 17, sets `options.release = 17`, enables ForgeGradle reobfuscation, makes `reobfJar` the production artifact task, and adds the `JEM-Recompiled-Target` and `JEM-Upstream-Commit` manifest fields. It does not change Java source, resources, registries, gameplay or network behavior

Prepare the exact source without a build:

```powershell
python Mods/SiftBackport/prepare.py
```

Build the production artifact only when the user authorizes the Gradle build:

```powershell
python Mods/SiftBackport/prepare.py --build --java-home C:/Users/user/.jdks/ms-17.0.15
```

The result is `Mods/SiftBackport/build/libs/Sift-Backport-1.0.5.1-1.20.1-forge-java17.jar`. The distributed JAR contains Forge 1.20.1 SRG runtime names rather than Mojmap development names. Tests and gameplay launches are not part of this build route
