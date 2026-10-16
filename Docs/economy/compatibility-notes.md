# Forge / Forgified Fabric API native trade compatibility — 2026-10-16

Status: **Bug / Both Sided / Forge 47.4.10 adds an Item local before the
ItemStack captured by the bundled FFAPI type-aware trade injection / in testing**.

## Evidence and cause

The ordinary dedicated server reached `Done` at 02:51:28. Invoking an ordinary
vanilla villager trade listing through the requested economy export then crashed
at 02:51:32 (`Server/logs/latest.log`, `crash-2026-10-16_02.51.33-server.txt`).
The exporter merely exposed the same provider used by normal villagers; catching
that exception or omitting the provider would leave normal gameplay broken.

The installed dependency chain is:

`txnilib-forge-1.0.24-1.20.1.jar`
→ `META-INF/jars/fabric-api-0.92.2+1.11.12+1.20.1.jar`
→ `META-INF/jarjar/fabric-object-builder-api-v1-11.1.3+4bd998fa77.jar`.

No Connector JAR is involved in this chain. This is the actual bundled Forgified
Fabric API port. Its exact bytecode shows:

- `fabric-object-builder-v1.mixins.json` registers
  `net.fabricmc.fabric.mixin.object.builder.TradeOffersTypeAwareBuyForOneEmeraldFactoryMixin`.
- `failOnNullItem(Entity, RandomSource, CallbackInfoReturnable, ItemStack)` injects
  before `new MerchantOffer`, with `LocalCapture.CAPTURE_FAILEXCEPTION`.
- The callback assumes the first captured local is ItemStack. It returns null for
  an empty stack. Its constructor redirect disables complete-villager-type checks.

The exact Forge `forge-1.20.1-47.4.10-userdev.jar` patch
`patches/net/minecraft/world/entity/npc/VillagerTrades.java.patch` already disables
that constructor validation and changes `EmeraldsForVillagerTypeItem.getOffer` to:

1. Obtain `Item item = trades.get(villagerType)`.
2. Return null if the modded type has no mapped item.
3. Construct `ItemStack itemstack = new ItemStack(item, cost)`.
4. Construct the unchanged native MerchantOffer.

The runtime diagnostic confirms captured locals `[Item, ItemStack]`, while the
FFAPI callback expects `[ItemStack]`. `require=0` does not make an incompatible
captured local safe: Mixin installs a failure callback, which throws when the
provider is first executed.

Five Whys: the trade request failed because its provider executed that failure
callback; the callback exists because local capture mismatched; the local layout
differs because Forge intentionally added the missing-type Item guard; the
bundled port retained Fabric's original local signature; the pack lacked an
installed-version compatibility boundary for that redundant incompatible patch.

## Source correction

JEMCompat now uses the **existing installed MixinSquared 0.2.0-beta.6 public
MixinCanceller SPI**, supplied by Nightfall, to cancel exactly the bad FFAPI
Mixin on exactly `VillagerTrades$EmeraldsForVillagerTypeItem`, and only when the
object-builder module version is exactly `11.1.3+4bd998fa77`. A future module
version, another mixin, another target, or a changed multi-target declaration is
left untouched and must be reviewed independently.

The SPI descriptor is
`META-INF/services/com.bawnorton.mixinsquared.api.MixinCanceller` and points to
`com.siirio.jemcompat.compat.fabric.FabricTradeMixinCanceller`. Its predicate is
plain Java and has a six-assertion opt-in regression executable.

Forge retains ownership of missing-type null handling and constructor behavior.
A tiny `FabricTypeAwareTradeMixin` HEAD injection preserves FFAPI's distinct
mapped-`Items.AIR` rejection, using only the authoritative trades map and the
villager's native type. It captures no locals, performs no monetary operation,
and changes no valid goods, quantities, NBT, stock, XP or offers. Official 1.20.1
mappings identify its field as `trades` and method as
`getOffer(Entity, RandomSource): MerchantOffer`; production mapping must be
confirmed as `m_213663_` in the built refmap.

This corrects an existing native compatibility fault on both physical sides,
including integrated singleplayer. It adds no shared-server gameplay to
singleplayer. Installed third-party JARs are not modified.

## Build and validation boundary

JEMCompat still requires `-Pjem_instance=<absolute Client/ManagedPack path>` and its
existing sibling module compiled classes. A build-only extraction task reads:

`EpicFight*Nightfall-3.4.1*.jar` (runtime filename encodes its space differently between profiles)
→ `META-INF/jarjar/mixinsquared-forge-0.2.0-beta.6.jar`
→ `META-INF/jars/MixinSquared-0.2.0-beta.6.jar`.

Only `build/compile-only/MixinSquared-0.2.0-beta.6.jar` is written for compileOnly
resolution. Nothing is bundled, installed or newly required at runtime. Missing
or changed audited nested paths fail the build explicitly. Incremental compilation
is disabled so annotation-generated refmaps are retained on repeated builds.

Root integration owns the build and
`compatTradePolicyContractTest`, SPI descriptor/refmap inspection, fresh local
artifact installation, ordinary dedicated-server restart and native provider
export. This worker ran no build/test/runtime operation. Success requires both
the version-bound cancellation appearing in the actual runtime and the full
native-provider export completing; a successful startup alone does not establish
that the lazy trade provider works. Root also owns Atlas coverage, CHANGELOG,
AutoModpack publication and final delivery.

Root integration confirmed the SPI descriptor and production getOffer mapping m_213663_, passed six policy assertions and booted the ordinary server. Its explicit audit completed 643 native provider entries with no provider failure; the previously crashing native type-aware factory was sampled successfully. This establishes the specific lazy-factory repair, while player trading acceptance remains separate.
