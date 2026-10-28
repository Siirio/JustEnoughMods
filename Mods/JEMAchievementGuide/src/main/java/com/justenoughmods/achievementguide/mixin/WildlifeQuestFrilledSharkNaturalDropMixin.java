package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSources;
import com.github.alexthe666.alexsmobs.entity.EntityFrilledShark;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityFrilledShark", remap = false)
public abstract class WildlifeQuestFrilledSharkNaturalDropMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityFrilledShark;m_19998_(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$natural(EntityFrilledShark entity, ItemLike item) {
        ItemEntity drop = entity.spawnAtLocation(item); WildlifeQuestSources.nativeDrop(drop, "wildlife_tooth", null); return drop;
    }
}
