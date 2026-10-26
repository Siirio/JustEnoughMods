# Tweaked Original Mods

This file lists upstream mod artifacts whose original code or packaged JAR was changed by JEM. Integration through a separate JEM adapter, datapack, configuration, or Mixin does not mark the upstream JAR itself as tweaked.

## AutoModpack 5.0.0-rc.1

- Artifact: `jem-bootstrap.jar`
- Upstream revision: `4348bf2532bb8261af3297cf2480a3a0ee91befb`
- JEM revision: `jem.4`
- Why: preserve the native first-install/download/restart flow while fixing bootstrap recovery and keeping AutoModpack as the only owner of its managed client generation.
- How: the client bootstrap no longer rejects a first installation before AutoModpack can create its manifest; recovery uses the untouched installer profile, and the packaged JAR records both the upstream commit and JEM revision in its manifest.
- Compatibility: the upstream `5.0.0-rc.1` network/version contract is retained so the server does not replace the patched bootstrap with an upstream artifact.

## Not counted as tweaked originals

JEM integrations for Create, Iron's Spells, Workers, Recruits, Xaero, Tide, PlayerRevive, TinyInv, Jade, Immersive Aircraft, Trotting Wagons, Immersive Melodies and other installed mods live in separate JEM modules, configs, tags or datapacks. Their upstream JARs remain unchanged and therefore have no `Tweaked` status in [MODS.md](MODS.md). BackToBed was removed from the canonical pack; `/home` is owned by JEM Server instead of a patched upstream artifact.

JEM Claims and Flan are removed from the intended runtime and source ownership. Settlement territories remain informational gameplay regions and do not protect land; boss arenas and active event safety remain with World Boss Tiers and the hosted-event lifecycle.
