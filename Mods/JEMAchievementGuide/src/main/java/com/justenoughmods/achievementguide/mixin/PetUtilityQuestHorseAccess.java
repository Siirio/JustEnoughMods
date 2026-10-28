package com.justenoughmods.achievementguide.mixin;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.SimpleContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractHorse.class)
public interface PetUtilityQuestHorseAccess {@Accessor("inventory")SimpleContainer jem$horseInventory();}
