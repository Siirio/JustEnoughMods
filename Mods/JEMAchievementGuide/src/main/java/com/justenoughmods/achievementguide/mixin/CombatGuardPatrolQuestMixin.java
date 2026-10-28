package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.combattools.CombatGuardProof;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import tallestegg.guardvillagers.entities.Guard;
import tallestegg.guardvillagers.networking.GuardSetPatrolPosPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo
@Mixin(targets = "tallestegg.guardvillagers.networking.GuardSetPatrolPosPacket", remap = false)
public abstract class CombatGuardPatrolQuestMixin {
    @Inject(method = "lambda$handle$0", at = @At("HEAD"))
    private static void jem$begin(Supplier<NetworkEvent.Context> context, GuardSetPatrolPosPacket message, CallbackInfo callback) {
        ServerPlayer actor = context.get().getSender();
        Guard guard = actor != null && actor.level().getEntity(message.getEntityId()) instanceof Guard actual ? actual : null;
        CombatGuardProof.patrolBegin(actor, guard, message.isPressed());
    }
    @Inject(method = "lambda$handle$0", at = @At("RETURN"))
    private static void jem$end(Supplier<NetworkEvent.Context> context, GuardSetPatrolPosPacket message, CallbackInfo callback) { CombatGuardProof.patrolEnd(); }
}
