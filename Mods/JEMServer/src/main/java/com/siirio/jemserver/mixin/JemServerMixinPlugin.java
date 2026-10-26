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
        if (mixinClassName.startsWith(SHARED_MAP_MIXINS)) return loaded(SHARED_MAP_MOD);
        if (mixinClassName.startsWith(XAERO_MIXINS))
            return loaded("xaeroworldmap") && (!mixinClassName.endsWith("OfficialWaypointTeleportMixin") || loaded("xaerominimap"));
        if (mixinClassName.contains(".fishing.")) return loaded("tide");
        if (mixinClassName.contains(".revive.")) return loaded("playerrevive");
        if (targetClassName.startsWith("com.github.L_Ender.cataclysm.")) return loaded("cataclysm");
        if (targetClassName.startsWith("net.unusual.block_factorys_bosses.")) return loaded("block_factorys_bosses");
        if (targetClassName.startsWith("top.theillusivec4.curios.")) return loaded("curios");
        if (targetClassName.startsWith("de.maxhenkel.gravestone.")) return loaded("gravestone");
        if (targetClassName.startsWith("com.jack.gravestones.")) return loaded("jacksgravestones");
        if (targetClassName.startsWith("net.mcreator.deepdarkregrowth.")) return loaded("deep_dark_regrowth");
        if (targetClassName.startsWith("net.satisfy.")) {
            String suffix = targetClassName.substring("net.satisfy.".length());
            int separator = suffix.indexOf('.');
            return separator > 0 && loaded(suffix.substring(0, separator));
        }
        return true;
    }

    private static boolean loaded(String mod) {
        return FMLLoader.getLoadingModList().getModFileById(mod) != null;
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
