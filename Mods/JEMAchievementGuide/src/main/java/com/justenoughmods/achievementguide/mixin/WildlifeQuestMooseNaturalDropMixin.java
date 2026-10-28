package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestSources;
import com.github.alexthe666.alexsmobs.entity.EntityMoose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityMoose", remap = false)
public abstract class WildlifeQuestMooseNaturalDropMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lcom/github/alexthe666/alexsmobs/entity/EntityMoose;m_19983_(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity jem$natural(EntityMoose entity, ItemStack item) {
        ItemEntity drop = entity.spawnAtLocation(item); WildlifeQuestSources.nativeDrop(drop, "wildlife_antler", null); return drop;
    }
}
