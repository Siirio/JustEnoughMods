package com.siirio.jemworldbosstiers.mixin;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ExperienceOrb.class)
public interface ExperienceOrbInvoker {
    @Invoker("repairPlayerItems")
    int jem$repairPlayerItems(Player player, int experience);
}
