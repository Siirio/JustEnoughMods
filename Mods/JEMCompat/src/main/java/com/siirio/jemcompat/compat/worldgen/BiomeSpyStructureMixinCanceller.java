package com.siirio.jemcompat.compat.worldgen;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import java.util.List;
import net.minecraftforge.fml.loading.FMLLoader;

/** Keeps Structurify's placement-aware structure locator in charge. */
public final class BiomeSpyStructureMixinCanceller implements MixinCanceller {
    @Override
    public boolean shouldCancel(List<String> targets, String mixin) {
        if (!mixin.equals("com.moepus.biomespy.mixin.ChunkGeneratorMixin")
                || !targets.contains("net.minecraft.world.level.chunk.ChunkGenerator")) {
            return false;
        }
        var mods = FMLLoader.getLoadingModList();
        var biomeSpy = mods.getModFileById("biomespy");
        return mods.getModFileById("structurify") != null
                && biomeSpy != null
                && biomeSpy.getMods().stream().anyMatch(mod ->
                        mod.getModId().equals("biomespy") && mod.getVersion().toString().equals("1.3.3"));
    }
}
