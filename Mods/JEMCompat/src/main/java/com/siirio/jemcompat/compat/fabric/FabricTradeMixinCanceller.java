package com.siirio.jemcompat.compat.fabric;

import com.bawnorton.mixinsquared.api.MixinCanceller;
import java.util.List;
import net.minecraftforge.fml.loading.FMLLoader;

/** MixinSquared's installed public SPI; no third-party JAR is patched or replaced. */
public final class FabricTradeMixinCanceller implements MixinCanceller {
    @Override public boolean shouldCancel(List<String> targets, String mixin) {
        if (!FabricTradeMixinPolicy.MIXIN.equals(mixin)) return false;
        var file = FMLLoader.getLoadingModList().getModFileById("fabric_object_builder_api_v1");
        if (file == null) return false;
        String version = file.getMods().stream().filter(mod -> mod.getModId().equals("fabric_object_builder_api_v1"))
                .map(mod -> mod.getVersion().toString()).findFirst().orElse("");
        return FabricTradeMixinPolicy.shouldCancel(targets, mixin, version);
    }
}
