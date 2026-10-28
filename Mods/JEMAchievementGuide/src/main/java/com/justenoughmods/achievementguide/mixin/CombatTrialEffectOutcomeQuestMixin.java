package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatEffectProof;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "net.salju.trialstowers.events.TrialsEvents", remap = false)
public abstract class CombatTrialEffectOutcomeQuestMixin {
    @Inject(method = "onHurt", at = @At("HEAD"))
    private static void jem$hurtBegin(LivingHurtEvent event, CallbackInfo callback) { CombatEffectProof.outcomeBegin(event.getEntity(), event.getSource().getEntity(), event, false); }
    @Inject(method = "onHurt", at = @At("RETURN"))
    private static void jem$hurtEnd(LivingHurtEvent event, CallbackInfo callback) { CombatEffectProof.outcomeEnd(); }
    @Inject(method = "onDeath", at = @At("HEAD"))
    private static void jem$deathBegin(LivingDeathEvent event, CallbackInfo callback) { CombatEffectProof.outcomeBegin(event.getEntity(), event.getSource().getEntity(), event, true); }
    @Inject(method = "onDeath", at = @At("RETURN"))
    private static void jem$deathEnd(LivingDeathEvent event, CallbackInfo callback) { CombatEffectProof.outcomeEnd(); }
    @Redirect(method = {"onHurt", "onDeath"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$spawn(ServerLevel level, Entity output) { boolean accepted = level.addFreshEntity(output); CombatEffectProof.spawned(output, accepted); return accepted; }
    @Redirect(method = "onHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_7731_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean jem$web(Level level, BlockPos pos, BlockState state, int flags) { boolean before = level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.COBWEB); boolean accepted = level.setBlock(pos, state, flags); CombatEffectProof.web(level, pos, state, accepted && !before); return accepted; }
}
