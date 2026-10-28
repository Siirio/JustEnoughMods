package com.justenoughmods.achievementguide.mixin;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTrigger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CriterionTrigger.Listener.class)
public interface NativeQuestListenerAccess {
    @Accessor("advancement") Advancement jem$questAdvancement();
}
