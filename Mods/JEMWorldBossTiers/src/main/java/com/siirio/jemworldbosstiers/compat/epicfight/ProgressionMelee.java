package com.siirio.jemworldbosstiers.compat.epicfight;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

/** Loaded only when Epic Fight is present; its physical source retains the action's item. */
public final class ProgressionMelee {
    private ProgressionMelee() { }

    public static ItemStack usedWeapon(DamageSource source) {
        return source instanceof EpicFightDamageSource epic && epic.getUsedItem() != null
                ? epic.getUsedItem() : ItemStack.EMPTY;
    }
}
