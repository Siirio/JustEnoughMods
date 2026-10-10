package com.siirio.jemcompat;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OptionalMixins implements IMixinConfigPlugin {
    private static final Map<String, List<String>> DEPENDENCIES = Map.ofEntries(
            Map.entry("com.siirio.jemcompat.mixin.events.LuminousEventVictoryMixin", List.of("luminous_beasts")),
            Map.entry("com.siirio.jemcompat.mixin.backtobed.MagicalReturnerMixin", List.of("backtobed")),
            Map.entry("com.siirio.jemcompat.mixin.guide.SpawnWithJourneyGuideProcedureMixin", List.of("spawn")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.EnhancedAiSpawningMixin", List.of("enhancedai", "jem_world_boss_tiers")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.KrakenStaggerMixin", List.of("block_factorys_bosses")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.SandwormStaggerMixin", List.of("block_factorys_bosses")),
            Map.entry("com.siirio.jemcompat.mixin.worldtier.CaptainCorneliaHealingMixin", List.of("aquamirae")),
            Map.entry("com.siirio.jemcompat.mixin.transport.EngineVehicleFuelMixin", List.of("immersive_aircraft")),
            Map.entry("com.siirio.jemcompat.mixin.transport.RoyalWagonInventoryMixin", List.of("trotting_wagons")),
            Map.entry("com.siirio.jemcompat.mixin.transport.TrottingWagonClydesdaleMixin", List.of("trotting_wagons", "barnyardbuddies")),
            Map.entry("com.siirio.jemcompat.mixin.client.IngredientListOverlayAccessor", List.of("jei")),
            Map.entry("com.siirio.jemcompat.mixin.client.StorageTerminalFocusMixin", List.of("toms_storage"))
    );

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return DEPENDENCIES.getOrDefault(mixinClassName, List.of()).stream()
                .allMatch(mod -> FMLLoader.getLoadingModList().getModFileById(mod) != null);
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
