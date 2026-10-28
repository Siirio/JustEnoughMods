package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.NativeQuestRecipients;
import com.justenoughmods.achievementguide.integration.food.WetHayPlacementProof;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.satisfy.beachparty.core.block.entity.WetHayBaleBlockEntity;
import net.satisfy.beachparty.core.registry.ObjectRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WetHayBaleBlockEntity.class, remap = false)
public abstract class WetHayQuestMixin {
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean jem$nativeDrying(ServerLevel level, BlockPos pos, BlockState result, int flags,
                                            ServerLevel nativeLevel, BlockPos nativePos, BlockState before, WetHayBaleBlockEntity owner) {
        UUID actor = owner.getPersistentData().hasUUID(WetHayPlacementProof.ACTOR)
                ? owner.getPersistentData().getUUID(WetHayPlacementProof.ACTOR) : null;
        boolean drying = actor != null && level == nativeLevel && pos.equals(nativePos)
                && level.getBlockEntity(pos) == owner && level.getBlockState(pos).equals(before)
                && before.is(ObjectRegistry.WET_HAY_BLOCK.get()) && result.is(ObjectRegistry.THATCH.get());
        boolean committed = level.setBlock(pos, result, flags);
        if (drying && committed && level.getBlockState(pos).equals(result)) {
            owner.getPersistentData().remove(WetHayPlacementProof.ACTOR);
            NativeQuestRecipients.accepted(level.getServer(), actor, "v3_food_thatch",
                    new ResourceLocation("jem_advancements", "quest_v3/v3_food_thatch"));
        }
        return committed;
    }
}
