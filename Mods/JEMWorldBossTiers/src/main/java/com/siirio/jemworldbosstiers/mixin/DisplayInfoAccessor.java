package com.siirio.jemworldbosstiers.mixin;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayInfo.class)
public interface DisplayInfoAccessor {
    @Mutable
    @Accessor("description")
    void jemWorldBossTiers$setDescription(Component description);
}
