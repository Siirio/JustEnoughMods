# JEM Food

Standalone Minecraft 1.20.1 Forge mod, Java 17, mod ID `jemfood`. Minecraft and Forge are its only mandatory dependencies. Install the matching JAR on client and logical server. Optional food assignments stay dormant when their items are absent. JEMCompat is not required

The single Atlas authority is `Catalog/changes/food-quality-assignments.json`. The build projects it into `data/jemfood/food_quality.json`. Do not maintain a second assignment table in this source project

Build inside this repository with `gradlew.bat :JEMFood:jar`. The module's own wrapper also supports `gradlew.bat jar`. `exportStandaloneSource` creates a source ZIP with the exact authority copied into its `catalog` directory at export time. That export builds with Forge alone, without a neighboring JEM module or cooking-mod JAR

One Food Blessing holds the authored talents. Food properties, native effects, recipes and containers remain upstream-owned. Native Resistance is separate from the custom protection budget, with the short authored duration and cooldown. Preferred optional effects remain disabled until their actual semantics are proved, using the explicit Minecraft fallback

All player-facing durations use ceiling-rounded whole seconds or whole minutes below one Minecraft day. Simulation, synchronization, effects and cooldowns remain in native ticks

See [food documentation](../../Docs/food/README.md) for migration, native consumption receipts and verification limits
