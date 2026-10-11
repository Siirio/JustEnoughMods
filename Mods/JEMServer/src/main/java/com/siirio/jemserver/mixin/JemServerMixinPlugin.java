package com.siirio.jemserver.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class JemServerMixinPlugin implements IMixinConfigPlugin {
    private static final String CLIENT_MIXINS = "com.siirio.jemserver.mixin.client.";
    private static final String XAERO_MIXINS = "com.siirio.jemserver.mixin.xaero.";
    private static final String SHARED_MAP_MIXINS = XAERO_MIXINS + "shared.";
    private static final String SHARED_MAP_MOD = "unified_xaero_worldmap";
    private static final String SHARED_BOUNDARY_MIXIN = "com.siirio.jemserver.mixin.SolidBoundaryMixin";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(SHARED_MAP_MIXINS)) {
            return FMLLoader.getLoadingModList().getModFileById(SHARED_MAP_MOD) != null;
        }
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER
                || mixinClassName.startsWith(CLIENT_MIXINS)
                || mixinClassName.startsWith(XAERO_MIXINS)
                || mixinClassName.equals(SHARED_BOUNDARY_MIXIN);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
