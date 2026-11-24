# JEI Catalog Manifest

## Problem

The old JEI catalog recomputed every assignment from the live `ItemStack` registry when JEI started. The classifier used item classes, tags, names, recipe catalysts, block state, and rule order. A pack change or a new rule could therefore move an item that the player had already approved.

## Decision

`jemcompat` owns one checked-in catalog manifest. The manifest assigns each known item registry ID to exactly one leaf category. The runtime reads this manifest and performs a direct registry-ID lookup. It does not infer a new category from an item's current class, name, tags, recipes, or block state.

The leaf categories are:

- `WEAPON_TOTEMS`
- `WEAPON_TRAINING`
- `WEAPON_RANGED`
- `WEAPON_MELEE`
- `WEAPON_SHIELDS`
- `WEAPON_SMITHING_TEMPLATES`
- `WEARABLE_ARMOR`
- `WEARABLE_MOB_ARMOR`
- `WEARABLE_DOLLS` (`Cosmetics` / `Косметика` in the interface)
- `FOOD_EDIBLES`
- `FOOD_COOKING_EQUIPMENT`
- `ORES`
- `MUSIC_DISCS`
- `TREES`
- `CREATE_REDSTONE`
- `BUILDING_FURNITURE`
- `BUILDING_NATURE`
- `BUILDING_CONTAINERS`
- `BUILDING_HOLIDAY`
- `BUILDING_BLOCKS`
- `BUILDING_GLASS`
- `BUILDING_LIGHT`
- `FISHING`
- `BOOKS`
- `POTIONS`
- `TRANSPORT`
- `COMPANIONS`
- `BUILDING_TOOLS`
- `SPAWN_EGGS`
- `OTHER`

`WEAPONS`, `WEARABLES`, `FOOD`, and `BUILDING` are navigation parents. They never own item assignments. A parent displays the union of its child leaf lists.

The interface name of `WEARABLE_DOLLS` is `Cosmetics` / `Косметика`. This leaf contains exact doll IDs and reviewed decorative mob heads that have no separate gameplay effect. Doll crafting stations and gift boxes remain construction furniture. The doll adjustment tool remains `OTHER`.

## Assignment contract

- One item registry ID has one leaf assignment
- An item can also appear in an exact additional view without changing its primary assignment
- Adding or removing a category does not reassign an existing ID
- A new item that is not in the manifest appears in `OTHER` and is reported once in the log
- Invalid categories and duplicate IDs make manifest loading fail with an explicit error
- User-approved assignments from `CHANGELOG.md` take precedence in the initial manifest
- Weapon subcategories split only the items that the earlier catalog classified as weapons
- Companion and transport spawn eggs keep `SPAWN_EGGS` as their primary assignment and also appear in their exact companion or transport view
- Vanilla and modded smithing templates, armor trims and smithing augments use `WEAPON_SMITHING_TEMPLATES`
- Creature drops and crafting parts without a more specific role use `OTHER`. The retired Components group has no runtime category
- Every installed item tagged as a playable music disc uses the top-level `MUSIC_DISCS` leaf
- Logs, planks, wooden architectural families, signs, doors, trapdoors, shelves, trims and reviewed wooden furniture use Building > `TREES`. Leaves, soil, saplings and living plants remain in Building > Nature. Casual Swing remains together in Building > Furniture and Decorations
- Create machinery, train equipment and residual Create-family items use `CREATE_REDSTONE`. Reviewed decorative palettes, copper roof and tile sets, secret buttons, Locometal color blocks and other building-only families use Building > Building Blocks. Semantic weapons, armor, food, fishing supplies and ores keep their matching leaves
- A flower-tagged placeable item cannot remain in Food only because a recipe consumes it. Reviewed harvest plants such as nettle, plantain, wild wheat and wild lettuce remain Food because harvesting them supplies cooking ingredients
- `BUILDING_LIGHT` contains light sources. Color prefixes such as `light_blue` and `light_gray` do not make ordinary wool, concrete, terracotta, chalk or kaolin a light source
- JEM Economy reads the same manifest through the existing optional JEMCompat contract

## Maintenance

`tools/generate_jei_catalog.py` creates the initial manifest from an exact logical-server registry export and the approved offline priority rules. The 2026-11-22 export contains 13,869 item IDs and has SHA-256 `958be5aee64f564c148764bdb9e97a8115c3e00fd44c0fe9b65fcd5f98174d39`. After the initial conversion, the checked-in manifest is authoritative. Update mode preserves all existing assignments and assigns only previously unknown IDs to `OTHER`. Reclassify mode applies the reviewed corrections through 2026-11-24 with Registry Atlas records, checks all required exact moves and rejects any smithing template or non-semantic Create item left outside its approved leaf. Validate mode rejects missing, unknown, duplicate or invalid assignments without changing the manifest. Review mode regenerates the namespace-count report from the checked-in manifest.

This policy prevents a generator update from rewriting accepted categories. A future category correction changes the exact item IDs in the manifest and records the decision in `CHANGELOG.md` and the Registry Atlas change history. The 2026-11-23 review added exact Archie's Dinosaurs and Royal Variations assignments, moved verified weapon and utility examples, retired Components, and added Cosmetics, Music Discs and Trees presentation. The 2026-11-24 review moved Trees under Building and corrected the reviewed decorative block, wood, plant, flower, textile and non-weapon component families.

## Runtime behavior

JEI still enumerates its live item stacks because it owns the actual visible variants. JEMCompat normalizes each preview stack and performs one constant-time manifest lookup by item registry ID. It no longer scans recipe categories or executes the semantic classifier for every stack.

NBT variants of one registry ID share one category. A future variant-specific requirement needs a separately approved manifest schema revision.

Schema 2 adds `additional_views`. These lists are exact. Runtime code does not infer tameability, rideability, or companion behavior from item names. Generic taming foods are not duplicated. Only reviewed unique companion items and reviewed spawn eggs appear in `COMPANIONS`. Rideable creature eggs appear in `TRANSPORT`.
