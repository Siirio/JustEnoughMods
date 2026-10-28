package com.justenoughmods.achievementguide.mixin;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractFurnaceMenu.class)
public interface WorldPrehistoricQuestFurnaceMenuAccess {@Accessor("container")Container jem$prehistoricFurnace();}
