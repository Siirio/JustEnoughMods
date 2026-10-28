# JEM Excavation

JEM Excavation adds toggleable, permission-safe area mining for Forge 1.20.1. Level I mines a 3x3 plane and level II mines a 7x7 plane while retaining native tool suitability, durability and Unbreaking behavior

The enchantment remains registered as `jem_pack_core:excavation`, and the disabled-level item tag remains `JEMExcavationDisabledLevel`, so existing enchanted tools keep their identity and toggle state

## Build

Run `./gradlew clean build` with Java 17. The build resolves Forge and Mixin tooling from public repositories and does not read a local Minecraft profile

See [LICENSE](LICENSE) for the rights covering only original project code and assets
