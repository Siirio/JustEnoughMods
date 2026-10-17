package com.siirio.jemsettlements.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class SettlementMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String name) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.contains(".xaero.")) return FMLEnvironment.dist == Dist.CLIENT
                && net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("xaeroworldmap") != null;
        if (mixin.endsWith(".PlayerSaveAccess") || mixin.endsWith(".SharedChestLidMixin")) return true;
        if (net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("recruits") == null) return false;
        return true;
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
