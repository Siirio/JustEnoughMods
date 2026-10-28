package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.GingerbreadManEntity;
import com.github.alexmodguy.alexscaves.server.block.blockentity.GingerbarrelBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.GingerbreadManStoreStolenItemsGoal.class,remap=false)
public abstract class CaveQuestGingerStorageMixin {
    @Shadow @Final private GingerbreadManEntity mob;
    @Unique private net.minecraft.server.level.ServerPlayer jem$caveDispatchActor;
    @Unique private ItemStack jem$caveDispatchRemainder;
    @Unique private GingerbarrelBlockEntity jem$caveDispatchBarrel;
    @Inject(method="m_8037_",at=@At("HEAD"))private void jem$begin(CallbackInfo ci){jem$caveDispatchActor=null;jem$caveDispatchRemainder=null;jem$caveDispatchBarrel=null;}
    @Inject(method="m_8037_",at=@At("RETURN"))private void jem$committedDispatch(CallbackInfo ci){
        if(jem$caveDispatchActor!=null && jem$caveDispatchBarrel!=null && !jem$caveDispatchBarrel.isRemoved()
                && ItemStack.matches(mob.getOffhandItem(),jem$caveDispatchRemainder)){
            CaveQuestProof.emit(jem$caveDispatchActor,"v3_cave_gingerbread_dispatch");mob.getPersistentData().remove("jemCavePickedV4");}
        jem$caveDispatchActor=null;jem$caveDispatchRemainder=null;jem$caveDispatchBarrel=null;
    }
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/HopperBlockEntity;m_59326_(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack jem$nativeDispatch(Container from,Container to,ItemStack offered,Direction side){
        ItemStack template=offered.copy();int before=CaveQuestProof.count(to,template);var proof=mob.getPersistentData().getCompound("jemCavePickedV4");
        ItemStack result=HopperBlockEntity.addItem(from,to,offered,side);int moved=template.getCount()-result.getCount();var actor=CaveQuestProof.owner(mob,"jemCaveOvenActorV4");
        if(actor!=null && mob.isOvenSpawned() && to instanceof GingerbarrelBlockEntity barrel && mob.position().distanceToSqr(barrel.getBlockPos().getCenter())<=2.25
                && template.save(new CompoundTag()).equals(proof) && mob.getOffhandItem().save(new CompoundTag()).equals(proof) && moved>0 && CaveQuestProof.count(to,template)-before==moved){
            jem$caveDispatchActor=actor;jem$caveDispatchRemainder=result.copy();jem$caveDispatchBarrel=barrel;}
        return result;
    }
}
