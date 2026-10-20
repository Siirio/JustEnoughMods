package com.siirio.jemworldbosstiers;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** New combat adapters are physically server-only; optional Iron targets are version-pinned. */
public final class PowerMixins implements IMixinConfigPlugin {
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".IronsSiphonHealingMixin")) {
            var file = FMLLoader.getLoadingModList().getModFileById("irons_spellbooks");
            return FMLLoader.getDist().isDedicatedServer() && file != null
                    && file.getMods().stream().anyMatch(mod -> mod.getModId().equals("irons_spellbooks")
                    && mod.getVersion().toString().equals("1.20.1-3.15.6"));
        }
        if (mixin.endsWith(".CombatProbeMixin") || mixin.endsWith(".ProgressionGuardMixin"))
            return FMLLoader.getDist().isDedicatedServer();
        return true;
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
