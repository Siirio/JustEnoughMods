package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge's physical placed block entity stores its real placing actor through ordinary native saves. */
@Mod.EventBusSubscriber(modid = JemAdvancementGuide.MOD_ID)
public final class WetHayPlacementProof {
    public static final String ACTOR = "jemNativeWetHayPlacerV1";
    private WetHayPlacementProof() {}
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer
                || player.isSpectator() || player.getAbilities().instabuild || event.isCanceled()
                || !BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString().equals("beachparty:wet_hay_block")) return;
        BlockEntity owner = player.level().getBlockEntity(event.getPos());
        if (owner != null && owner.getBlockState().equals(event.getPlacedBlock())) {
            owner.getPersistentData().putUUID(ACTOR, player.getUUID());
            owner.setChanged();
        }
    }
}
