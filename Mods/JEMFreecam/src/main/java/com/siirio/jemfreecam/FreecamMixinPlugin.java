package com.siirio.jemfreecam;

import java.util.List;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class FreecamMixinPlugin implements IMixinConfigPlugin {
    private static final String EPIC_CAMERA = "com.siirio.jemfreecam.mixin.FreeCameraEpicFightCameraMixin";
    private static final String EPIC_INPUT = "com.siirio.jemfreecam.mixin.FreeCameraEpicFightInputMixin";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.equals(EPIC_CAMERA) && !mixinClassName.equals(EPIC_INPUT)) {
            return true;
        }
        var file = FMLLoader.getLoadingModList().getModFileById("epicfight");
        if (file == null) {
            return false;
        }
        boolean supported = file.getMods().stream().anyMatch(mod -> mod.getModId().equals("epicfight")
                && mod.getVersion().toString().equals("20.14.17"));
        if (!supported) {
            throw new IllegalStateException("JEM Freecam requires an input adapter review for this Epic Fight version");
        }
        return true;
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
