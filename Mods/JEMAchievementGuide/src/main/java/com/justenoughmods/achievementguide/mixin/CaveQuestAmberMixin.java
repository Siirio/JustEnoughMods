package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.block.blockentity.AmberMonolithBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AmberMonolithBlockEntity.class,remap=false)
public abstract class CaveQuestAmberMixin {
    @Redirect(method="spawnMobs",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;m_47205_(Lnet/minecraft/world/entity/Entity;)V"))
    private void jem$acceptedAnimal(ServerLevel level,Entity animal){
        level.addFreshEntityWithPassengers(animal);
        var owner=(BlockEntity)(Object)this;var tag=owner.getPersistentData();
        if(level.getEntity(animal.getUUID())==animal && animal.isAlive() && (animal instanceof net.minecraft.world.entity.animal.Animal || animal instanceof net.minecraft.world.entity.animal.WaterAnimal)
                && tag.hasUUID(CaveQuestProof.OWNER)){tag.putUUID("jemCaveAmberPendingV4",tag.getUUID(CaveQuestProof.OWNER));owner.setChanged();var actor=CaveQuestProof.owner(owner);
            if(actor!=null){CaveQuestProof.emit(actor,"v3_cave_amber_nursery");tag.remove("jemCaveAmberPendingV4");}}
    }
    @Inject(method="tick",at=@At("RETURN"))private static void jem$nativePending(net.minecraft.world.level.Level world,net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state,AmberMonolithBlockEntity owner,CallbackInfo ci){
        if(!(world instanceof ServerLevel level) || !owner.getPersistentData().hasUUID("jemCaveAmberPendingV4"))return;
        var actor=CaveQuestProof.actor(level,owner.getPersistentData().getUUID("jemCaveAmberPendingV4"));
        if(actor!=null){CaveQuestProof.emit(actor,"v3_cave_amber_nursery");owner.getPersistentData().remove("jemCaveAmberPendingV4");owner.setChanged();}
    }
}
