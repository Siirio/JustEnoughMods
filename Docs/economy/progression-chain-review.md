# Progression-chain review

The runtime owner is the single `ItemPolicyRegistry`: only `BUY_AND_SELL` supplies both quotes; `NONE` supplies neither. Recipe distance alone never changes policy. Generic diamonds, netherite and ordinary materials remain eligible for explicit review even when they occur in a protected recipe; exclusive boss/rare-mob gates and their genuinely exclusive descendants fail closed.

## Current reviewed protected endpoints

- `alexsmobs:cooked_catfish`
- `aquamirae:cooked_spinefish`
- `bakery:apple_cupcake`
- `bakery:baguette`
- `bakery:bun`
- `beachparty:cooked_mussel_meat`
- `cnc:cooked_game`
- `cnc:cooked_venison`
- `kaleidoscope_cookery:apple_platter`
- `kaleidoscope_cookery:baozi`
- `minecraft:baked_potato`
- `minecraft:bread`
- `minecraft:cobblestone`
- `minecraft:cooked_beef`
- `minecraft:cooked_cod`
- `minecraft:cooked_salmon`
- `minecraft:emerald`
- `minecraft:netherrack`
- `minecraft:red_sand`
- `minecraft:rotten_flesh`
- `minecraft:sand`
- `minecraft:stone`
- `oceanicrealms:cooked_mackerel_fish`
- `primal:apple_fritter`
- `primal:cooked_venison`

## Evidence boundary

`Catalog/graphs/recipes/` preserves static recipe evidence. Exclusive transitive descendants are not inferred automatically because alternative acquisition and ordinary independent uses require human review. Ambiguous entries stay `NONE`/unresolved rather than contaminating generic ancestors or receiving fabricated prices.
