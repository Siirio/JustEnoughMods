# Transport and Carriage Plan

Status: source implementation candidate on `feature/transport-carriage`. It is not merged into `develop`, built, published, installed, or accepted in gameplay.

This document records the current transport direction for the modpack. It separates verified current behavior, user decisions, and numeric implementation proposals.

## Design target

Each transport system must keep a useful role:

- Minecarts provide fast and reliable travel on built land routes
- Mounts let a loaded player avoid the on-foot backpack penalty and travel without fixed infrastructure
- The biplane provides expensive flexible long-range travel
- Elytra provides flexible personal flight with a takeoff and boost budget
- Backpacks provide convenient mixed-item storage with an on-foot movement cost
- Shulker boxes provide dense bulk block transport without portable-container nesting

## Current verified baseline

| Method | Current top speed | Planned result |
| --- | ---: | ---: |
| Walking | About 4.32 blocks/s | Backpack load can reduce it by up to 70% |
| Sprinting | About 5.61 blocks/s | Backpack load can reduce it by up to 70% |
| Sprint-jumping | About 7.13 blocks/s | Backpack load can reduce it by up to 70% |
| Standard vanilla minecart | 8 blocks/s | 18 blocks/s on straight level rail |
| Fueled furnace minecart | 10 blocks/s from current JEM logic | Use the same route caps as other vanilla minecarts |
| Boat on water | About 8 blocks/s | No change |
| Boat on normal or packed ice | About 40 blocks/s | No change is approved |
| Boat on blue ice | About 72.7 blocks/s | No change is approved |
| Horse | About 4.74 to 14.23 blocks/s | Native values remain |
| Donkey or mule | About 7.53 blocks/s | Native values remain |
| Immersive Aircraft biplane | 26 blocks/s JEM cap | Keep 26 blocks/s |
| Create train | Native Create behavior | Keep native Create behavior |
| Elytra | Variable flight speed | No speed cap, but apply takeoff and boost limits |

The 18 blocks/s minecart target is 2.25 times the vanilla cap. It covers 1,000 straight blocks in about 56 seconds. It is faster than common mounts, but it is not faster than the biplane or native ice boats.

The plan therefore defines minecarts as the fastest safe automated land corridor. Making them the highest raw-speed method would require more than 72.7 blocks/s because blue-ice boats already reach that speed. That value would make rail curves, chunk loading, and arrival handling unsafe.

## Backpacks and on-foot burden

### Removed upgrades

Remove these portable-backpack mechanics from active play:

- `sophisticatedbackpacks:inception_upgrade`
- `sophisticatedbackpacks:stack_upgrade_starter_tier`
- `sophisticatedbackpacks:stack_upgrade_tier_1`
- `sophisticatedbackpacks:stack_upgrade_tier_2`
- `sophisticatedbackpacks:stack_upgrade_tier_3`
- `sophisticatedbackpacks:stack_upgrade_tier_4`
- `sophisticatedbackpacks:stack_upgrade_omega_tier`
- All conversion recipes that create or convert those backpack upgrades

The Inception upgrade lets one backpack contain and combine access to other backpacks. Stack upgrades multiply the number of items that one backpack slot can hold. The user first considered a limit of one stack upgrade and then replaced that direction with complete removal of Inception and all backpack stack upgrades.

This scope applies to Sophisticated Backpacks. It does not remove Sophisticated Storage stack upgrades from stationary storage unless the user expands the scope later.

Removal preserves existing backpack possessions:

- Extract nested backpacks recursively with their full NBT
- Split oversized backpack stacks into native-size stacks
- Return removed upgrade items and overflow through durable JEM Rewards delivery
- Never delete or ground-drop overflow during migration
- Disable the recipes, upgrade effects, upgrade-slot admission, and ordinary recipe-view exposure after migration

The user confirmed that no player owns a shulker box. The shulker capacity change therefore has no legacy migration or old-state preservation path. Existing backpack migration remains necessary because the removed Inception and stack upgrades can contain player-owned items.

### Exact movement formula

Burden starts with the first occupied backpack because the user's full-netherite example requires one backpack to cause a 40% reduction.

`movement reduction = min(70%, total occupied backpack slots × 0.333... percentage points)`

Each occupied storage slot adds one third of one percent. The item count inside a slot does not matter. All carried and equipped portable backpacks add to the same total.

| Full backpack tier | Storage slots | Movement reduction |
| --- | ---: | ---: |
| Leather | 27 | 9% |
| Copper | 45 | 15% |
| Iron | 54 | 18% |
| Gold | 81 | 27% |
| Diamond | 108 | 36% |
| Netherite | 120 | 40% |

A full netherite backpack changes the approximate movement values to 2.59 blocks/s walking, 3.37 blocks/s sprinting, and 4.28 blocks/s sprint-jumping. The 70% hard cap leaves 30% of normal speed.

Apply the reduction to walking, sprinting, sprint-jumping, and sneaking on foot. Do not apply it to riding, vehicle movement, swimming, climbing, elytra flight, or creative flight. This gives mounts a direct cargo role without changing their native speed or storage.

Use one server-owned transient movement-speed attribute modifier instead of the vanilla Slowness potion effect. Milk, beacons, and other potion mechanics must not remove or replace the cargo rule.

### Backpack burden indicator

Show one backpack icon near the hotbar while the player carries or equips at least one backpack.

- Fill the icon from bottom to top by occupied slots divided by total available backpack slots
- Change the fill color from green through yellow and orange to red by the actual 0% to 70% movement reduction
- Show the movement reduction to one decimal place when needed, such as `-0.3%`, and use whole percentages such as `-40%` when exact
- Synchronize only when the occupied-slot result or carried capacity changes
- Use one bounded logical-server inventory scan every five player ticks and never scan inventory during HUD rendering

## Shulker boxes

Inside a shulker box, a block item whose native maximum stack size is 64 can stack to 128. Other item limits remain native.

- One full 27-slot shulker can hold 3,456 blocks
- This equals 54 ordinary 64-item stacks
- Shulker box items still stack to one
- Items that are not 64-stack block items keep their native limit
- Shift-click, hopper, pickup, save, load, tooltip, and screen synchronization must preserve counts above 64
- Extraction into a container that cannot accept 128 must split the stack without loss

Portable containers cannot nest in either direction. Backpacks cannot enter shulker boxes, and shulker boxes cannot enter backpacks. This is the Codex balance consequence that prevents both systems from multiplying each other after Inception is removed.

## Minecarts and rails

### Minecart item and recipe

- `minecraft:minecart` stacks to 16
- Its recipe remains five iron ingots in the native U shape for one minecart
- Chest, hopper, TNT, and furnace minecart item stack limits remain native until the user expands the rule

### Route speed

Apply these caps to vanilla minecart entities on vanilla rail shapes:

- 18 blocks/s on straight level rail
- 14 blocks/s while ascending
- 12 blocks/s through curves
- Keep native acceleration, collision, braking, activator-rail, detector-rail, and unpowered-rail behavior unless later inspection proves a required compatibility change
- Exclude Create trains and Create-owned moving contraptions

The cap limits speed. It must not force a stopped cart to move. Powered rails, furnace propulsion, gravity, collision, and player input remain the motion sources.

### Rail cost proposal

The user selected iron ingots for normal rails and gold ingots for powered rails. These are already the native materials. The proposed economy change is therefore output quantity, not a new ingredient family.

- Six iron ingots and one stick produce 32 rails instead of 16
- Six gold ingots, one stick, and one redstone produce 16 powered rails instead of 6
- Detector and activator rail recipes remain native

These approved output counts make a 1,000-block main line cost about 188 iron ingots before powered-rail spacing and station equipment.

## Elytra

### Takeoff

Remove the earlier proposal that required 12 unobstructed air blocks below the player.

- The player must accumulate at least six blocks of vanilla fall distance before elytra deployment is accepted
- Deployment before six blocks is rejected
- At or after six blocks, the player uses the normal activation input
- Do not auto-deploy without player input
- Do not add an air-clearance raycast
- Apply the rule to every item that actually grants elytra flight, including compatible modded chest items, instead of using a vanilla-item-only list

### Firework boost budget

Use a batch budget, not a rolling window:

- Start with eight available elytra boosts
- Each successful elytra firework boost consumes one charge and one rocket
- Charges do not regenerate while one to seven charges remain
- The eighth successful boost changes the budget to zero and starts one two-minute cooldown
- When the cooldown ends, restore all eight charges at once
- A denied boost consumes neither a charge nor a rocket
- Landing does not reset or refill the budget
- Non-flight firework launching, crossbows, crafting, and combat use do not use this budget

Recommended server state:

- `boostsRemaining` from zero through eight
- `refillAtEpochMillis` only while `boostsRemaining` is zero
- Persist both values across logout, death, dimension transfer, and server restart
- Use a wall-clock deadline so relogging or a normal restart cannot reset the wait and time offline still counts
- Validate and consume the rocket on the logical server, then synchronize the result to the client

## Mounts

No new horse, donkey, mule, or llama speed increase is approved in this update.

- Horses keep their native individual speed range
- Donkeys keep the existing JEM six-column chest with 18 cargo slots. Mules keep their native 15 cargo slots
- Llamas keep their native 3 to 15 storage slots and caravan behavior
- The existing JEM llama catch-up bonus remains 30% when a caravan llama is more than six blocks behind its leader
- Mounted movement ignores the backpack on-foot penalty

Existing JEM caps and adapters for other rideable entities and wagons remain unchanged unless this document names them. This update gives mountables a stronger relative role through the backpack on-foot exclusion instead of another broad speed increase.

This gives mounts an immediate job. A player with one full netherite backpack moves 40% slower on foot but keeps the mount's full speed while riding.

## Aircraft

Only the Immersive Aircraft biplane is in active aircraft scope.

- Keep the current JEM horizontal cap of 26 blocks/s
- Keep 16 fuel units of capacity
- Keep 90 seconds of powered operation per fuel unit
- Full fuel capacity provides 24 minutes of powered operation
- Keep the current maximum powered ascent of 8 blocks/s
- Do not add another aircraft to this update

Aircraft rules belong to universal modpack gameplay in JEMCompat. The logical server enforces them in both integrated and dedicated worlds. They must not become a JEMServer-only feature.

## Explicit non-changes

- Do not change TPA behavior, cooldowns, commands, or ownership
- Do not change Create train speed
- Do not buff ice boats
- Do not cap elytra flight speed in this plan

## Implementation ownership

- JEMCompat owns universal transport, backpack burden, shulker capacity, minecart, elytra, and aircraft adapters
- Data or configuration owns recipes when it can express them without Java
- Native mod interfaces and signals remain in control where available
- Server authority validates gameplay state and clients render synchronized presentation
- The Registry Atlas change record and effective behavior must be updated in the same implementation task
- The burden HUD reuses the installed native backpack item rendering with a code-drawn clipped fill and text. It adds no production PNG or replacement screen
- Runtime HUD geometry, color, legibility, and scaled-screen behavior remain unobserved until the candidate is built and opened in Minecraft
