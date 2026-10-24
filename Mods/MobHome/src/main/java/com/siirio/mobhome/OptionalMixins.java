package com.siirio.mobhome;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Select each optional target before Mixin links its upstream API types. */
public final class OptionalMixins implements IMixinConfigPlugin {
    private static final Map<String, String> OWNERS = Map.ofEntries(
            Map.entry("com.github.alexthe666.alexsmobs.", "alexsmobs"),
            Map.entry("com.github.alexmodguy.alexscaves.", "alexscaves"),
            Map.entry("dev.xylonity.companions.", "companions"),
            Map.entry("io.redspace.ironsspellbooks.", "irons_spellbooks"),
            Map.entry("com.hm.efn.", "efn"),
            Map.entry("org.primal.", "primal"),
            Map.entry("com.ninni.spawn.", "spawn"),
            Map.entry("tech.alexnijjar.endermanoverhaul.", "endermanoverhaul"),
            Map.entry("com.github.L_Ender.cataclysm.", "cataclysm"),
            Map.entry("net.imasillylittleguy.cnc.", "cnc"));

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        if (!mixin.startsWith("com.siirio.mobhome.mixin.shelter.alex.")
                && !mixin.startsWith("com.siirio.mobhome.mixin.shelter.pack.")) return true;
        // This one adapter observes native Entity age, with its own exact-instance predicate.
        if (mixin.endsWith(".PackSummonAgeMixin")) return true;
        if (mixin.endsWith(".CompanionsRespawnOwnerMixin") && !loaded("knightlib")) return false;
        return OWNERS.entrySet().stream().anyMatch(owner -> target.startsWith(owner.getKey()) && loaded(owner.getValue()));
    }

    private static boolean loaded(String mod) {
        return FMLLoader.getLoadingModList().getModFileById(mod) != null;
    }

    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
