# Mob Home

Standalone Forge 1.20.1 mod using Java 17. Runtime requirements are Minecraft and Forge only. Complete singleplayer gameplay lives on the integrated server. Multiplayer needs the mod on both clients and server

Canonical gameplay and compatibility documentation: ../../Docs/companions/README.md

Build from this folder with `gradlew.bat clean build`

Optional integrations resolve from public Maven repositories for compilation. Their JARs are not bundled and none is mandatory at runtime. Output: `build/libs/mobhome-1.0.0.jar`

Two editable native Blockbench houses and layered Aseprite atlases are retained in `../../Docs/art/mobhome-20261024`. World rendering uses static per-cell baked models, one 128x64 atlas per house and cutout foliage. Native export packaging and model-derived collision are documented in `tools/README.md`. No block-entity renderer or render ticker is required

The stone house retains the selected visual direction. The log cabin uses continuous walls without gaps between courses. Runtime compatibility and player acceptance remain separate from source/build and editor evidence. See `../../Docs/companions/delivery.json` for the actual artifact and local installation state

Registry IDs: mobhome:small_animal_home, mobhome:large_animal_home

The earlier external models were rejected. These replacement models follow the user's explicit request for direct native production
