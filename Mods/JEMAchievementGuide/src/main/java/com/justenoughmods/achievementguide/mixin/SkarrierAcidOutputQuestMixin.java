package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.SkarrierWorkshopProof;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Pseudo
@Mixin(targets = "net.mcreator.skarriermobs.procedures.AcidicBileRightclickedOnBlockProcedure", remap = false)
public abstract class SkarrierAcidOutputQuestMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean jem$output(ServerLevel world, Entity output) {
        boolean accepted = world.addFreshEntity(output);
        SkarrierWorkshopProof.spawned(output, accepted);
        return accepted;
    }
}
