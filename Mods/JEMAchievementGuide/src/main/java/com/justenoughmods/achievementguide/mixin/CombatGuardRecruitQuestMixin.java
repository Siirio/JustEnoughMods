package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatGuardProof;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "tallestegg.guardvillagers.VillagerToGuard", remap = false)
public abstract class CombatGuardRecruitQuestMixin {
    @Inject(method = "onEntityInteract", at = @At("HEAD"))
    private static void jem$begin(PlayerInteractEvent.EntityInteract event, CallbackInfo callback) { CombatGuardProof.begin(event); }
    @Redirect(method = "convertVillager", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$spawn(Level level, Entity entity) { boolean accepted = level.addFreshEntity(entity); CombatGuardProof.spawned(entity, accepted); return accepted; }
    @Inject(method = "onEntityInteract", at = @At("RETURN"))
    private static void jem$end(PlayerInteractEvent.EntityInteract event, CallbackInfo callback) { CombatGuardProof.end(); }
}
