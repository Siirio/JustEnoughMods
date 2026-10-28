package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestRitual;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=net.mcreator.luminousnether.procedures.RitualOfMendingButtonProcedure.class,remap=false)
public abstract class WorldRitualQuestRitualOfMendingButtonProcedureMixin {
    @Inject(method="execute",at=@At("HEAD"))private static void jem$begin(LevelAccessor world,double x,double y,double z,Entity actor,CallbackInfo ci){WorldRitualQuestRitual.begin(actor,x,y,z,"mending","7");}
    @Redirect(method="execute",at=@At(value="INVOKE",target="Lnet/minecraft/world/inventory/Slot;m_6201_(I)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack jem$paid(Slot slot,int count){return WorldRitualQuestRitual.remove(slot,count);}
    @Inject(method="execute",at=@At("RETURN"))private static void jem$end(LevelAccessor world,double x,double y,double z,Entity actor,CallbackInfo ci){WorldRitualQuestRitual.end();}
}
