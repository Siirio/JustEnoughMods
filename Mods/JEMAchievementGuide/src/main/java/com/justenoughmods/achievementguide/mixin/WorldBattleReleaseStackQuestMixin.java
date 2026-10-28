package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.battle.*;import net.minecraft.world.item.ItemStack;import net.minecraft.nbt.CompoundTag;import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(ItemStack.class) public abstract class WorldBattleReleaseStackQuestMixin implements WorldBattleExtraStackCarrier {
 @Unique private CompoundTag jem$release;
 @Override public CompoundTag jem$battleRelease(){return jem$release==null?new CompoundTag():jem$release.copy();}
 @Override public void jem$battleRelease(CompoundTag value){jem$release=value==null?null:value.copy();}
 @Inject(method="<init>(Lnet/minecraft/nbt/CompoundTag;)V",at=@At("RETURN")) private void jem$read(CompoundTag tag,CallbackInfo ci){if(tag.contains("jemBattleReleaseStackV4",10))jem$release=tag.getCompound("jemBattleReleaseStackV4").copy();}
 @Inject(method="save",at=@At("RETURN")) private void jem$save(CompoundTag tag,CallbackInfoReturnable<CompoundTag> ci){var value=WorldBattleExtraMovement.stackReceipt((ItemStack)(Object)this);if(!value.isEmpty())ci.getReturnValue().put("jemBattleReleaseStackV4",value);}
 @Inject(method="copy",at=@At("RETURN")) private void jem$copy(CallbackInfoReturnable<ItemStack> ci){var value=WorldBattleExtraMovement.stackReceipt((ItemStack)(Object)this);if(!value.isEmpty())((WorldBattleExtraStackCarrier)(Object)ci.getReturnValue()).jem$battleRelease(value);}
 @Inject(method="setCount",at=@At("HEAD")) private void jem$count(int count,CallbackInfo ci){if(count!=((ItemStack)(Object)this).getCount())jem$release=null;}
}
