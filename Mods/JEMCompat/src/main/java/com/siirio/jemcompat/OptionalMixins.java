package com.siirio.jemcompat;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OptionalMixins implements IMixinConfigPlugin {
    private static final Map<String, List<String>> DEPENDENCIES = Map.ofEntries(
            Map.entry("com.siirio.jemcompat.mixin.food.KaleidoscopeFoodBiteMixin", List.of("kaleidoscope_cookery")),
            Map.entry("com.siirio.jemcompat.mixin.grave.LegacyGraveCreationMixin", List.of("jacksgravestones", "gravestone")),
            Map.entry("com.siirio.jemcompat.mixin.trading.FabricTypeAwareTradeMixin", List.of("fabric_object_builder_api_v1")),
            Map.entry("com.siirio.jemcompat.mixin.events.LuminousEventVictoryMixin", List.of("luminous_beasts")),
            Map.entry("com.siirio.jemcompat.mixin.guide.SpawnWithJourneyGuideProcedureMixin", List.of("spawn")),
            Map.entry("com.siirio.jemcompat.mixin.seasons.SpawnSunflowerSeasonsMixin", List.of("spawn", "sereneseasons")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.EnhancedAiSpawningMixin", List.of("enhancedai", "jem_world_boss_tiers")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.KrakenStaggerMixin", List.of("block_factorys_bosses")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.SandwormStaggerMixin", List.of("block_factorys_bosses")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.CaptainCorneliaHealingMixin", List.of("aquamirae")),
            Map.entry("com.siirio.jemcompat.mixin.transport.EngineVehicleFuelMixin", List.of("immersive_aircraft")),
            Map.entry("com.siirio.jemcompat.mixin.transport.AircraftInventoryMixin", List.of("immersive_aircraft")),
            Map.entry("com.siirio.jemcompat.mixin.transport.AircraftFuelSlotMixin", List.of("immersive_aircraft")),
            Map.entry("com.siirio.jemcompat.mixin.transport.AircraftLiftMixin", List.of("immersive_aircraft")),
            Map.entry("com.siirio.jemcompat.mixin.backpack.BackpackInventoryProcessMixin", List.of("backpacked", "framework")),
            Map.entry("com.siirio.jemcompat.mixin.backpack.BackpackWorkbenchMenuMixin", List.of("backpacked", "framework")),
            Map.entry("com.siirio.jemcompat.mixin.backpack.BackpackAugmentAdmissionMixin", List.of("backpacked", "framework")),
            Map.entry("com.siirio.jemcompat.mixin.backpack.BackpackWorkbenchScreenMixin", List.of("backpacked", "framework")),
            Map.entry("com.siirio.jemcompat.mixin.transport.RoyalWagonInventoryMixin", List.of("trotting_wagons")),
            Map.entry("com.siirio.jemcompat.mixin.transport.TrottingWagonClydesdaleMixin", List.of("trotting_wagons", "barnyardbuddies")),
            Map.entry("com.siirio.jemcompat.mixin.client.IngredientListOverlayAccessor", List.of("jei")),
            Map.entry("com.siirio.jemcompat.mixin.client.StorageTerminalFocusMixin", List.of("toms_storage"))
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.login.ServerLoginPublicChatMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("serverlogin");
            return FMLLoader.getDist().isDedicatedServer() && file != null && file.getMods().stream()
                    .anyMatch(mod -> mod.getModId().equals("serverlogin") && mod.getVersion().toString().equals("1.0.3"));
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.login.ClosedConnectionErrorMixin")) {
            var file=FMLLoader.getLoadingModList().getModFileById("serverlogin");
            return FMLLoader.getDist().isDedicatedServer()&&file!=null&&file.getMods().stream()
                    .anyMatch(mod->mod.getModId().equals("serverlogin")&&mod.getVersion().toString().equals("1.0.3"));
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.login.TerminalConnectionFailureMixin")
                || mixinClassName.equals("com.siirio.jemcompat.mixin.login.ConnectivityTerminalSocketMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("connectivity");
            return FMLLoader.getDist().isDedicatedServer() && file != null
                    && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("connectivity")
                    && mod.getVersion().toString().equals("1.20.1-7.6"));
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.worldtier.FrostbittenDifficultyMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("legendary_monsters");
            return file != null
                    && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("legendary_monsters")
                    && mod.getVersion().toString().equals("1.20.1"));
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.client.recruits.RecruitDebugScreenPoseMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("recruits");
            return file != null && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("recruits")
                    && mod.getVersion().toString().equals("1.15.2"));
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.login.ServerLoginRespawnPacketMixin")) {
            return FMLLoader.getDist().isDedicatedServer()
                    && FMLLoader.getLoadingModList().getModFileById("serverlogin") != null;
        }
        if (mixinClassName.equals("com.siirio.jemcompat.mixin.trading.FabricTypeAwareTradeMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("fabric_object_builder_api_v1");
            return file != null && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("fabric_object_builder_api_v1")
                    && mod.getVersion().toString().equals("11.1.3+4bd998fa77"));
        }
        boolean dependenciesPresent = DEPENDENCIES.getOrDefault(mixinClassName, List.of()).stream()
                .allMatch(mod -> FMLLoader.getLoadingModList().getModFileById(mod) != null);
        if (dependenciesPresent && mixinClassName.equals("com.siirio.jemcompat.mixin.seasons.SpawnSunflowerSeasonsMixin")) {
            var spawn = FMLLoader.getLoadingModList().getModFileById("spawn");
            // Mixin can merely warn about a missing target even in a required
            // config. Inspect the installed mod file without loading its class.
            if (!Files.isRegularFile(spawn.getFile().findResource(targetClassName.replace('.', '/') + ".class"))) {
                throw new IllegalStateException("Installed Spawn is missing " + targetClassName
                        + "; update the JEMCompat sunflower seasons adapter for this Spawn version.");
            }
        }
        return dependenciesPresent;
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
