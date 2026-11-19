package com.siirio.jemsettlements.mixin.recruits;

import com.talhanation.recruits.VillagerEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Workstation conversion is a body replacement, not a new family member. */
@Pseudo
@Mixin(value=VillagerEvents.class,remap=false)
public abstract class VillagerRecruitFamilyMixin {
    @Unique private static final ThreadLocal<Villager> jem$source=new ThreadLocal<>();
    @Inject(method="createRecruit",at=@At("HEAD")) private static void jem$begin(Villager villager,EntityType<?> type,CallbackInfo ci){jem$source.set(villager);}
    @Redirect(method="createRecruit",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/EntityType;create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",remap=true),remap=false)
    private static Entity jem$identity(EntityType<?> type,Level level){Entity entity=type.create(level);Villager source=jem$source.get();if(entity!=null&&source!=null){var data=source.getPersistentData();if(data.hasUUID("JemFamilyPerson"))entity.getPersistentData().putUUID("JemFamilyPerson",data.getUUID("JemFamilyPerson"));entity.getPersistentData().putString("JemRecruitProvenance","table");}return entity;}
    @Inject(method="createRecruit",at=@At("RETURN")) private static void jem$end(Villager villager,EntityType<?> type,CallbackInfo ci){jem$source.remove();}
}
