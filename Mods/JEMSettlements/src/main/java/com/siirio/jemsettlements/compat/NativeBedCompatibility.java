package com.siirio.jemsettlements.compat;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.mixin.PoiTypesAccess;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = JemSettlements.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class NativeBedCompatibility {
    private NativeBedCompatibility() { }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER) return;
        event.enqueueWork(() -> {
            var home = ForgeRegistries.POI_TYPES.getValue(PoiTypes.HOME.location());
            if (home == null) throw new IllegalStateException("Native HOME POI unavailable");
            for (var block : ForgeRegistries.BLOCKS.getValues()) {
                var id = ForgeRegistries.BLOCKS.getKey(block);
                if (id == null || !id.getNamespace().equals("handcrafted") || !(block instanceof BedBlock)) continue;
                for (var state : block.getStateDefinition().getPossibleStates())
                    if (state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.HEAD)
                        PoiTypesAccess.jem$states().putIfAbsent(state, home);
            }
        });
    }
}
