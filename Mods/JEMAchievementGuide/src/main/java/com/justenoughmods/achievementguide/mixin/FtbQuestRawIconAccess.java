package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(QuestObjectBase.class)
public interface FtbQuestRawIconAccess {
    @Accessor(value = "rawIcon", remap = false) ItemStack jem$getRawIcon();
}
