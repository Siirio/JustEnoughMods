package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.block.BlockSculkBoomer;
import com.github.alexthe666.alexsmobs.tileentity.TileEntitySculkBoomer;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.tileentity.TileEntitySculkBoomer", remap = false)
public abstract class WildlifeQuestBoomerMixin {
    @Shadow private int screamTime;
    @Unique private int jem$before;
    @Inject(method = "m_214068_", at = @At("HEAD"))
    private void jem$vibration(ServerLevel level, GameEvent event, GameEvent.Context context, Vec3 from, CallbackInfoReturnable<Boolean> callback) { jem$before = screamTime; }
    @Inject(method = "m_214068_", at = @At("RETURN"))
    private void jem$vibrated(ServerLevel level, GameEvent event, GameEvent.Context context, Vec3 from, CallbackInfoReturnable<Boolean> callback) {
        var self = (TileEntitySculkBoomer)(Object)this; var proof = WildlifeQuestSetups.get(level).at(level, self.getBlockPos());
        if (jem$before == 0 && screamTime == -20 && self.getBlockState().getValue(BlockSculkBoomer.POWERED) && proof.hasUUID("boomer_disabled_actor")) {
            proof.putBoolean("suppression_pending", true); WildlifeQuestSetups.get(level).put(level, self.getBlockPos(), proof);
        }
    }
    @Redirect(method = "commonTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private static boolean jem$effect(LivingEntity target, DamageSource source, float amount, Level level, BlockPos pos, BlockState state, TileEntitySculkBoomer self) {
        float before = target.getHealth(); boolean accepted = target.hurt(source, amount);
        if (accepted && target.getHealth() < before && level instanceof ServerLevel server) { var proof = WildlifeQuestSetups.get(server).at(server, pos); if (proof.hasUUID("actor")) { proof.putBoolean("boomer_effect", true); WildlifeQuestSetups.get(server).put(server, pos, proof); } }
        return accepted;
    }
    @Inject(method = "commonTick", at = @At("RETURN"))
    private static void jem$suppressed(Level level, BlockPos pos, BlockState state, TileEntitySculkBoomer self, CallbackInfo callback) {
        if (!(level instanceof ServerLevel server) || !self.getBlockState().getValue(BlockSculkBoomer.POWERED) || self.getBlockState().getValue(BlockSculkBoomer.OPEN)) return;
        var proof = WildlifeQuestSetups.get(server).at(server, pos);
        if (proof.getBoolean("boomer_effect") && proof.getBoolean("suppression_pending") && proof.hasUUID("boomer_disabled_actor") && self.saveWithoutMetadata().getInt("ScreamCooldown") < 0) {
            var actor = WildlifeQuestProof.real(server.getServer().getPlayerList().getPlayer(proof.getUUID("boomer_disabled_actor")));
            if (actor != null) { WildlifeQuestProof.emit(actor, "v3_fauna_redstone_creatures_boomer"); proof.remove("suppression_pending"); WildlifeQuestSetups.get(server).put(server, pos, proof); }
        }
    }
}
