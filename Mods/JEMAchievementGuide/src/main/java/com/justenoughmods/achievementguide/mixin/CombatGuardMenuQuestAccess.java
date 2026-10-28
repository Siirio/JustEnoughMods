package com.justenoughmods.achievementguide.mixin;
import tallestegg.guardvillagers.entities.Guard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
@Pseudo
@Mixin(targets = "tallestegg.guardvillagers.entities.GuardContainer", remap = false)
public interface CombatGuardMenuQuestAccess {
    @Accessor("guard") Guard jem$guard();
}
