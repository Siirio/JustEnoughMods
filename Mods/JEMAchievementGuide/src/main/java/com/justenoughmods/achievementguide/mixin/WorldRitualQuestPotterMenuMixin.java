package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=net.mcreator.luminousnether.world.inventory.PottergeistGUIMenu.class,remap=false)
public abstract class WorldRitualQuestPotterMenuMixin implements WorldRitualQuestPotter.MenuAccess {
    @Shadow private net.minecraft.world.entity.Entity boundEntity;
    @Override public net.minecraft.world.entity.Entity jem$ritualBoundEntity(){return boundEntity;}
}
