package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.simibubi.create.content.equipment.sandPaper.SandPaperItem", remap = false)
public abstract class SandpaperQuestMixin {
    @Redirect(method = "m_5922_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;m_150079_(Lnet/minecraft/world/item/ItemStack;)V", ordinal = 0))
    private void jem$committedPolish(Inventory inventory, ItemStack result) {
        boolean quartz = !result.isEmpty() && new ResourceLocation("create", "polished_rose_quartz")
                .equals(ForgeRegistries.ITEMS.getKey(result.getItem()));
        inventory.placeItemBackInInventory(result);
        if (quartz && inventory.player instanceof ServerPlayer actor) {
            QuestActionTrigger.INSTANCE.emit(actor, "create_polish_quartz");
        }
    }
}
