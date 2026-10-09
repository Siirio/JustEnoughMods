package com.siirio.jemtwelveeyes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class OptionalMixins implements IMixinConfigPlugin {
    private static final Map<String, List<String>> DEPENDENCIES = Map.ofEntries(
            Map.entry("com.siirio.jemcompat.mixin.compat.tradecycling.TradeCyclingModMixin", List.of("trade_cycling"))
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
