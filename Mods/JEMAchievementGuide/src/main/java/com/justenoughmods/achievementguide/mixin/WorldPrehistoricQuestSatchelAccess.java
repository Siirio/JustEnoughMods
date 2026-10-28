package com.justenoughmods.achievementguide.mixin;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="net.mcreator.unearthedjourney.world.inventory.LargeHideSatchelGUIMenu",remap=false)
public interface WorldPrehistoricQuestSatchelAccess {
 @Accessor("boundEntity")Entity jem$prehistoricBound();
 @Accessor("internal")IItemHandler jem$prehistoricStorage();
}
