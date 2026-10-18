package com.siirio.jemsettlements.compat;

import com.siirio.jemsettlements.JemSettlements;
import com.siirio.jemsettlements.mixin.PoiTypesAccess;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Extend the existing native HOME contract without replacing a registered record or its ticket policy. */
@Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class NativeBedCompatibility {
    private static volatile PoiType homeType;
    private static volatile Set<BlockState> supportedHeads=Set.of(),homeStates=Set.of();
    private NativeBedCompatibility() { }
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        if(net.minecraftforge.fml.loading.FMLEnvironment.dist!=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)return;
        event.enqueueWork(NativeBedCompatibility::extendNativeHome);
    }
    private static void extendNativeHome() {
        var home=ForgeRegistries.POI_TYPES.getValue(PoiTypes.HOME.location());
        if(home==null)throw new IllegalStateException("Native HOME POI unavailable");
        var accepted=new LinkedHashSet<BlockState>();
        var all=new LinkedHashSet<>(home.matchingStates());
        var reverse=PoiTypesAccess.jem$states();
        for(var block:ForgeRegistries.BLOCKS.getValues()) {
            var id=ForgeRegistries.BLOCKS.getKey(block);
            if(id==null||!id.getNamespace().equals("handcrafted")||!(block instanceof BedBlock))continue;
            for(var state:block.getStateDefinition().getPossibleStates()) {
                if(!state.hasProperty(BedBlock.PART)||state.getValue(BedBlock.PART)!=BedPart.HEAD)continue;
                var existing=reverse.putIfAbsent(state,home);
                if(existing!=null&&existing!=home)
                    throw new IllegalStateException("Handcrafted bed state already belongs to another POI: "+id);
                accepted.add(state);all.add(state);
            }
        }
        supportedHeads=Set.copyOf(accepted);homeStates=Set.copyOf(all);homeType=home;
    }
    public static boolean isHome(PoiType type) { return homeType!=null&&type==homeType; }
    public static boolean supportedHead(BlockState state) { return supportedHeads.contains(state); }
    public static Set<BlockState> homeStates() { return homeStates; }

    /** Forge can restore registry snapshots between server lifecycles; refresh the native reverse index before chunks load. */
    @Mod.EventBusSubscriber(modid=JemSettlements.MOD_ID,value=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER)
    public static final class ServerLifecycle {
        private ServerLifecycle() { }
        @SubscribeEvent public static void beforeStart(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
            if(event.getServer().isDedicatedServer())extendNativeHome();
        }
    }
}
