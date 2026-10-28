package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.*;
import com.github.alexmodguy.alexscaves.server.entity.living.TremorzillaEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=com.github.alexmodguy.alexscaves.server.entity.ai.AnimalLayEggGoal.class,remap=false)
public abstract class CaveQuestOptionalEggMixin {
    @Shadow @Final private Animal mob;
    @Redirect(method="m_8037_",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean jem$actualOffspringEgg(Level world,BlockPos pos,BlockState state){
        boolean result=world.setBlockAndUpdate(pos,state);var proof=mob.getPersistentData();
        if(result && world instanceof ServerLevel level && mob instanceof TremorzillaEntity && state.is(com.github.alexmodguy.alexscaves.server.block.ACBlockRegistry.TREMORZILLA_EGG.get())
                && level.getBlockState(pos).equals(state) && proof.hasUUID("jemCaveViableEggActorV4") && proof.hasUUID("jemCaveViableEggMateV4")){
            var origin=new net.minecraft.nbt.CompoundTag();origin.putBoolean("optional_breeding",true);origin.putUUID("actor",proof.getUUID("jemCaveViableEggActorV4"));origin.putUUID("parent",mob.getUUID());origin.putUUID("mate",proof.getUUID("jemCaveViableEggMateV4"));origin.putString("food","alexscaves:nuclear_bomb");origin.put("state",net.minecraft.nbt.NbtUtils.writeBlockState(state));CaveQuestBlockOrigins.put(level,pos,"Grown",origin);var actor=CaveQuestProof.actor(level,proof.getUUID("jemCaveViableEggActorV4"));if(actor!=null)CaveQuestProof.emit(actor,"v3_cave_giant_nursery_breeding");proof.remove("jemCaveViableEggActorV4");proof.remove("jemCaveViableEggMateV4");
        }return result;
    }
}
