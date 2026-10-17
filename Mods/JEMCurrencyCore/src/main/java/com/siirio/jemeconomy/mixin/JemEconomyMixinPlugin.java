package com.siirio.jemeconomy.mixin;

import java.util.List;
import java.util.Set;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Client keeps presentation compatibility; server hooks are gated by CurrencyAuthority before creating a session. */
public final class JemEconomyMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("CommercePlayerSaveAccess") || mixin.endsWith("CommerceServerStorageAccess"))
            return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("jem_economy") != null
                    && net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("jem_professions_sandbox") != null;
        return net.minecraftforge.fml.loading.FMLLoader.getLoadingModList().getModFileById("jem_economy") != null;
    }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
