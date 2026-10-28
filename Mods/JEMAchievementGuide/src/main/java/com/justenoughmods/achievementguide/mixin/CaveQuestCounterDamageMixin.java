package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.caves.CaveQuestProof;
import com.github.alexmodguy.alexscaves.server.entity.living.NotorEntity;
import com.github.alexmodguy.alexscaves.server.entity.living.MineGuardianEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class CaveQuestCounterDamageMixin {
    @Unique private final java.util.ArrayDeque<float[]> jem$caveHits=new java.util.ArrayDeque<>();
    @Unique private int jem$caveOverflow;
    @Inject(method="hurt",at=@At("HEAD"))private void jem$before(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        var self=(LivingEntity)(Object)this;if(!(self instanceof NotorEntity || self instanceof MineGuardianEntity))return;
        if(jem$caveOverflow>0 || jem$caveHits.size()>=32){jem$caveOverflow++;return;}
        boolean scan=self instanceof NotorEntity notor && notor.getScanningId()!=-1 && notor.getHologramUUID()==null;
        boolean armed=self instanceof MineGuardianEntity mine && mine.isExploding() && mine.getExplodeProgress(1)<1;
        jem$caveHits.push(new float[]{self.getHealth(),scan?1:0,armed?1:0});
    }
    @Inject(method="hurt",at=@At("RETURN"))private void jem$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> ci){
        var self=(LivingEntity)(Object)this;if(!(self instanceof NotorEntity || self instanceof MineGuardianEntity))return;
        if(jem$caveOverflow>0){jem$caveOverflow--;return;}if(jem$caveHits.isEmpty())return;var before=jem$caveHits.pop();var actor=CaveQuestProof.actor(source.getEntity());
        if(actor!=null && ci.getReturnValue() && Float.isFinite(amount) && amount>0 && self.getHealth()<before[0] && !self.isAlive()){
            if(before[1]>0 && self instanceof NotorEntity notor && notor.getHologramUUID()==null)CaveQuestProof.emit(actor,"v3_cave_cave_counterplay_notor");
            if(before[2]>0)CaveQuestProof.emit(actor,"v3_cave_cave_counterplay_mine");
        }
    }
}
