# Food quality and dietary variety

`Mods/JEMFood` is the sole food-quality owner on integrated and dedicated servers. Minecraft and Forge are its only mandatory dependencies. `JEMCompat` no longer registers food events, food networking, food Mixins, food resources or an assignment projection. The matching standalone JAR belongs on client and server

The user supplied [v3](v3-input/README_FIRST.md) and requested its complete implementation. The single assignment authority is [food-quality-assignments.json](../../Catalog/changes/food-quality-assignments.json), projected by Gradle unchanged into `data/jemfood/food_quality.json`. Its 848 exact IDs are Ordinary 320, Tasty 266, Exquisite 213 and Masterwork 49. There are 517 blessing meals, 11 elevated native-only meals and 569 variety-eligible IDs. Missing optional items remain dormant entries

## State and mechanics

One registered `jemfood:food_blessing` holds the exact authored one, two or three talents for 0.1, 0.15 or 0.2 Minecraft days. Ordinary food preserves the bundle. An elevated native-only meal clears it while native food effects remain intact. Re-eating the same source cannot refresh the bundle while more than one fifth of its original duration remains

The 30 talent coefficients come from that same authority. Movement, knockback, step height, health and attack speed use stable transient modifier UUIDs. No base attribute is rewritten and maximum-health increases grant no healing. Accepted native damage is adjusted once, without another attack. Custom outgoing bonuses have a 7% cap and are disabled against players. Custom projectile protection has a 10% cap and specialized hazards a 30% cap. Surefoot retains its explicit fall percentages. As clarified in the supplied effects contract, vanilla Resistance is a separate native effect with a short lifetime and cooldown

Fleetfoot, Swimmer and Stonehide currently use the explicit Speed I, Dolphin's Grace I and Resistance I fallback. Optional preferred IDs stay disabled until exact semantics and safe ownership are demonstrated. Hidden backend instances are tracked by server object identity. Standard external `addEffect` and `forceAddEffect` calls detach only an exact owned instance before native insertion. An independent same-ID source always wins. Nothing snapshots or restores its hidden chain. On the client only the synchronized exact hidden instance is excluded from the native inventory list, other native sources keep their icons

One scoped native receipt wraps `ServerPlayerGameMode` calling `BlockState.use`. It requires actual hunger insertion for that player, a consuming result and a changed portion/block state. Inventory food uses completed native item use. Creative, spectator, failed eating, predicted clicks, slicing and menu opening receive no bonuses. No third-party classes or optional Mixin targets are required. [Coverage](COVERAGE.md) lists the authored 22 block routes and actual installed recipe evidence, with unobserved routes explicitly unresolved

Miner, Digger and Woodcutter extend the native BreakSpeed hook on both prediction and logical server with valid tools. Harvester receives a receipt only after native `destroyBlock` succeeds for a mature CropBlock in the crops tag. Angler runs only after native fishing retrieval and all event listeners accept the catch. Sage changes only native nonboss XP, capped at two extra points. None creates another loot or XP-drop loop. Poison mitigation follows the actual native poison damage call, not unrelated magic damage while the player happens to be poisoned

Satiety and sprint-only Trailrunner use the larger applicable exhaustion reduction. Recovery talents keep persistent budgets. An earned Second Wind burst survives ordinary elevated-meal replacement under the same bundle owner, with one modifier and no timer extension during cooldown. Its remaining proc is synchronized even if the replacement dish has different talents. Native-only clearing, milk and lifecycle cleanup remove it

## Persistence and presentation

`JemFoodBlessingV2` owns schema 2, meal timestamps, cooldowns, counters and native-instance fingerprints. Migration from `JemFoodQualityV1` imports only existing permitted dietary IDs, never recreates an old food buff and removes an old native effect only when its complete serialized fingerprint matches. The old tag, including absorption debt, remains intact for rollback. This version grants no absorption

Death, clone, login, logout, dimension change and shutdown clear the current blessing, owned bridges and attributes. Variety timestamps and cooldowns survive. Milk and explicit blessing removal invalidate the bundle before a later re-add can revive it. Simulation uses overworld gameTime, independent of sleeping or changing dayTime. Invalid future timestamps cannot unlock a budget

The five most recent distinct permitted foods within three Minecraft days grant exactly one +5% movement-speed MULTIPLY_BASE modifier with the preserved UUID `766d2410-48f2-4223-bc78-d23860a9e004`. Keeping the most recent five is sufficient to decide the threshold without scanning a full dietary history

The bounded read-only `jemfood:food` protocol 2 sends the dietary count and actual active trait states. There is no gameplay mutation packet. Native Forge effect rendering records the actual 18 by 18 icon rectangle for hover, including other mods' horizontal offsets. The hover shows the source, quality, localized traits, active fallback, remaining durations and cooldowns. All newly displayed time is in Minecraft days. The four supplied tier colors and original supplied icon are retained. No new artwork or persistent HUD is authored

## Evidence and limits

The module builds independently with its own wrapper and no neighboring mod or cooking-mod API dependency. The standalone source export includes the exact Atlas authority at export time. [Delivery](delivery-v3.json) records actual production packaging, local runtime synchronization and native AutoModpack application

Project rules prohibit automated tests and gameplay launches for this request. The archive's testing and feature-branch instructions do not override those rules. No automated tests, test fixtures, client launch or player acceptance are claimed. Recipe-result extraction is evidence collection, not complete acquisition or rarity validation. Forge-only integrated gameplay, the optional-mod matrix, all bite routes, effect collisions, milk and inventory hover still require player observation. Stable profiles, MineRENT and the immutable release baseline remain outside this local delivery
