# JEM Animal Hints

JEM Animal Hints is a Jade addon for Forge 1.20.1 that shows taming items, breeding foods and verified mountability. Install it with Jade on both the client and server so Jade can obtain the authoritative animal data

## Requirements

- Minecraft 1.20.1

- Forge 47.4.10 or newer in the 47.x line

- Jade 11.13.3 or newer in the 11.x line

## Build

Run `./gradlew clean build` with Java 17. Dependencies resolve from public Maven repositories and the build does not read a local Minecraft profile

See [LICENSE](LICENSE) for the rights covering only original project code and assets

## Datapack extension

Additional species can be declared under `data/<namespace>/jemcompat/taming/*.json`. Each entry has an `entity`, an `items` array and an optional `mountable` boolean. Set `mountable` only when that exact registered species has a real riding path. Taming and mountability are intentionally independent

The Jade provider IDs and translation namespace remain `jemcompat:taming_item`, `jemcompat:breeding_item` and `jade.jemcompat.*` so existing Jade configuration remains compatible
