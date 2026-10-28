package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battlefinal.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.world.entity.Entity;import net.minecraft.world.level.LevelAccessor;import net.minecraft.world.item.ItemStack;import net.minecraftforge.eventbus.api.Event;
@Mixin(value=net.mcreator.luminousbeasts.procedures.GatorToothInHandProcedure.class,remap=false)
public abstract class BattleFinalQuestGatorToothInHandProcedureMixin {
    @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At("HEAD"))private static void jem$before(Event event,LevelAccessor world,double x,double y,double z,Entity target,Entity actor,CallbackInfo ci){BattleFinalQuestRelics.begin(event,actor,target,"gator");}
    @Redirect(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_220157_(ILnet/minecraft/util/RandomSource;Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private static boolean jem$cost(ItemStack stack,int amount,net.minecraft.util.RandomSource random,net.minecraft.server.level.ServerPlayer player){boolean result=stack.hurt(amount,random,player);BattleFinalQuestRelics.cost(stack);return result;}
    @Inject(method="execute(Lnet/minecraftforge/eventbus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)V",at=@At("RETURN"))private static void jem$after(Event event,LevelAccessor world,double x,double y,double z,Entity target,Entity actor,CallbackInfo ci){BattleFinalQuestRelics.end();}
}
