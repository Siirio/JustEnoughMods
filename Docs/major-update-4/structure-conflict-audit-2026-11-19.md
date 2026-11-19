# Structure conflict audit - 2026-11-19

Scope: exact JARs in the Test JEM managed pack after the requested removals and additions. This is a static registry and data-pack audit. It does not claim fresh-chunk gameplay acceptance.

| Area | Owners found | Result |
| --- | --- | --- |
| End Cities | Nullscape and Trek both define `minecraft:end_cities` | Unresolved overwrite. Data-pack order selects one structure-set definition |
| Villages | Luki's Grand Capitals, Nature's Spirit and Trek define `minecraft:villages` | Resolved by `jem-major-update-4-final-structure-compat`, which owns the merged set |
| Pillager Outposts | Luki's Grand Capitals and Trek define `minecraft:pillager_outpost` | Resolved by the same JEM compatibility data pack |
| Mineshafts | Hopo Better Mineshaft owns `minecraft:mineshaft`, `minecraft:mineshaft_mesa` and `minecraft:mineshafts` | No second mineshaft overhaul is installed. YUNG's Better Mineshafts is absent |
| Nether Fortresses | YUNG's Better Nether Fortresses, Jaden's Nether Expansion, Moog's Nether Structures and Luminous Nether | No exact registry overwrite found. YUNG replaces the vanilla fortress route, Jaden preserves fortress and bastion weights while adding its own structure, Moog adds `mns:mega_fortress`, and Luminous Nether only extends the fortress biome tag. These can increase fortress-like structure density without an ID conflict |
| Giant Torii | `gianttorii:giant_torii` in cherry groves | Separate structure and structure-set IDs. No exact conflict found |
| Structory | Removed | Its structures no longer add to general structure density |

The unresolved End City collision is not changed in this update. A later world-generation decision must select one owner or author an explicit merged definition.
