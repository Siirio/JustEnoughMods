package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.FoodDroppedProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.ai.PlatypusAIDigForItems", remap = false)
public abstract class WildlifeQuestPlatypusDigMixin {
    @Shadow private EntityPlatypus platypus;
    @Shadow private BlockPos digPos;
    @Redirect(method = "m_8037_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityPlatypus;m_19983_(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$dig(EntityPlatypus entity, ItemStack output) {
        ItemEntity drop = entity.spawnAtLocation(output); var actor = WildlifeQuestProof.actor(entity, "worker_actor");
        if (actor != null && drop != null && entity == platypus && digPos != null && entity.level().getBlockState(digPos).is(Blocks.CLAY)
                && entity.level() instanceof ServerLevel level && level.getEntity(drop.getUUID()) == drop) FoodDroppedProof.update(drop, "v3_fauna_animal_work", drop.getItem().getCount(), actor.getUUID());
        return drop;
    }
}
