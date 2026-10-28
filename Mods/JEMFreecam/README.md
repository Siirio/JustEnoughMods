# JEM Freecam

JEM Freecam is a client-only Forge 1.20.1 camera mode bound to F4. It moves a local camera entity through loaded collision space while the real player remains server-owned and stationary

Attack, use, pick, held mining and body movement are suppressed while the camera is active. Server teleports close the view. Epic Fight integration is optional and enabled only for the reviewed `20.14.17` contract

## Build

Run `./gradlew clean build` with Java 17. The build resolves Forge and Mixin tooling from public repositories and does not read a local Minecraft profile

See [LICENSE](LICENSE) for the rights covering only original project code and assets
