package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestRitual;
import org.spongepowered.asm.mixin.*;
import net.minecraft.world.level.block.entity.BlockEntity;
@Mixin(value=net.mcreator.luminousnether.world.inventory.RitualGUIMenu.class,remap=false)
public abstract class WorldRitualQuestMenuMixin implements WorldRitualQuestRitual.MenuAccess {
    @Shadow private BlockEntity boundBlockEntity;
    @Override public BlockEntity jem$ritualBlock(){return boundBlockEntity;}
}
