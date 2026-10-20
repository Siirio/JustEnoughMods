# Progression-chain review

The runtime owner is the single `ItemPolicyRegistry`: only `BUY_AND_SELL` supplies both quotes; `NONE` supplies neither. Recipe distance alone never changes policy. Generic diamonds, netherite and ordinary materials remain eligible for explicit review even when they occur in a protected recipe; exclusive boss/rare-mob gates and their genuinely exclusive descendants fail closed.

## Current reviewed protected endpoints

- `kaleidoscope_cookery:apple_platter`
- `minecraft:emerald`
- `minecraft:netherrack`
- `minecraft:red_sand`

## Evidence boundary

`Catalog/graphs/recipes/` preserves static recipe evidence. Exclusive transitive descendants are not inferred automatically because alternative acquisition and ordinary independent uses require human review. Ambiguous entries stay `NONE`/unresolved rather than contaminating generic ancestors or receiving fabricated prices.
