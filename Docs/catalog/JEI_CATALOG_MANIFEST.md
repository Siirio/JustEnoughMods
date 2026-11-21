# JEI Catalog Manifest

## Problem

The old JEI catalog recomputed every assignment from the live `ItemStack` registry when JEI started. The classifier used item classes, tags, names, recipe catalysts, block state, and rule order. A pack change or a new rule could therefore move an item that the player had already approved.

## Decision

`jemcompat` owns one checked-in catalog manifest. The manifest assigns each known item registry ID to exactly one leaf category. The runtime reads this manifest and performs a direct registry-ID lookup. It does not infer a new category from an item's current class, name, tags, recipes, or block state.

The leaf categories are:

- `WEAPON_TOTEMS`
- `WEAPON_RANGED`
- `WEAPON_MELEE`
- `WEARABLE`
- `FOOD`
- `ORES`
- `CREATE_REDSTONE`
- `FURNITURE`
- `NATURE`
- `CONTAINERS`
- `HOLIDAY`
- `BUILDING_BLOCKS`
- `GLASS`
- `LIGHT`
- `FISHING`
- `BOOKS`
- `POTIONS`
- `TRANSPORT`
- `SPAWN_EGGS`
- `OTHER`

`WEAPONS` and `BUILDING` are navigation parents. They never own item assignments. A parent displays the union of its child leaf lists.

## Assignment contract

- One item registry ID has one leaf assignment
- Adding or removing a category does not reassign an existing ID
- A new item that is not in the manifest appears in `OTHER` and is reported once in the log
- Invalid categories and duplicate IDs make manifest loading fail with an explicit error
- User-approved assignments from `CHANGELOG.md` take precedence in the initial manifest
- Weapon subcategories split only the items that the earlier catalog classified as weapons
- Protective death-prevention totems remain wearable unless an explicit approved assignment places them in a weapon leaf
- JEM Economy reads the same manifest through the existing optional JEMCompat contract

## Maintenance

`tools/generate_jei_catalog.py` creates the initial manifest from the Registry Atlas and the accepted legacy rules. After the initial conversion, the checked-in manifest is authoritative. The tool preserves all existing assignments and assigns only previously unknown IDs. It can also validate the manifest without changing it.

This policy prevents a generator update from rewriting accepted categories. A future category correction changes the exact item IDs in the manifest and records the decision in `CHANGELOG.md` and the Registry Atlas change history.

## Runtime behavior

JEI still enumerates its live item stacks because it owns the actual visible variants. JEMCompat normalizes each preview stack and performs one constant-time manifest lookup by item registry ID. It no longer scans recipe categories or executes the semantic classifier for every stack.

NBT variants of one registry ID share one category. A future variant-specific requirement needs a separately approved manifest schema revision.
