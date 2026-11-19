package com.siirio.jemsettlements.mixin.recruits;

import com.talhanation.recruits.RecruitEvents;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value=RecruitEvents.class,remap=false)
public abstract class RecruitPromotionFamilyMixin {
    @Unique private static final ThreadLocal<AbstractRecruitEntity> jem$source=new ThreadLocal<>();
    @Inject(method="promoteRecruit",at=@At("HEAD")) private static void jem$begin(AbstractRecruitEntity recruit,int profession,String name,ServerPlayer player,CallbackInfo ci){jem$source.set(recruit);}
    @Redirect(method="promoteRecruit",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",remap=true),remap=false)
    private static Entity jem$identity(EntityType<?> type,Level level){Entity entity=type.create(level);AbstractRecruitEntity source=jem$source.get();if(entity!=null&&source!=null){var data=source.getPersistentData();if(data.hasUUID("JemFamilyPerson"))entity.getPersistentData().putUUID("JemFamilyPerson",data.getUUID("JemFamilyPerson"));if(data.contains("JemRecruitProvenance"))entity.getPersistentData().putString("JemRecruitProvenance",data.getString("JemRecruitProvenance"));}return entity;}
    @Inject(method="promoteRecruit",at=@At("RETURN")) private static void jem$end(AbstractRecruitEntity recruit,int profession,String name,ServerPlayer player,CallbackInfo ci){jem$source.remove();}
}
