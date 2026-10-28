package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareLineage;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareProof;
import com.justenoughmods.achievementguide.integration.creaturecare.CreatureCareMouse.Release;
import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.imasillylittleguy.cnc.procedures.PotofmouseRightclickedProcedure", remap = false)
public abstract class CreatureCareMouseReleaseMixin {
    @Unique private static final ThreadLocal<Release> jem$release = new ThreadLocal<>();
    @Inject(method = "execute", at = @At("HEAD"))
    private static void jem$before(LevelAccessor world, double x, double y, double z, Entity player, ItemStack source, CallbackInfo callback) {
        jem$release.remove();
        if (player instanceof ServerPlayer actor && CreatureCareProof.real(actor) != null
                && CreatureCareProof.item(source).equals("cnc:potofmouse")) jem$release.set(new Release(actor, source));
    }
    @Redirect(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;m_262496_(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;)Lnet/minecraft/world/entity/Entity;"))
    private static Entity jem$spawn(EntityType<?> type, ServerLevel level, BlockPos pos, MobSpawnType reason) {
        Entity entity = type.spawn(level, pos, reason); Release release = jem$release.get();
        if (release != null && entity != null && CreatureCareProof.type(entity).equals("cnc:mouse")
                && level.getEntity(entity.getUUID()) == entity) release.released = entity;
        return entity;
    }
    @Inject(method = "execute", at = @At("RETURN"))
    private static void jem$after(LevelAccessor world, double x, double y, double z, Entity player, ItemStack source, CallbackInfo callback) {
        Release release = jem$release.get(); jem$release.remove();
        if (release == null || release.released == null || !CreatureCareProof.debited(release.before, source)) return;
        var physical = NativeFoodSources.get(release.actor.serverLevel());
        ItemStack spent = NativeFoodSources.spent(release.before, 1);
        UUID owner = physical.owner(spent, "creature_mouse_pot");
        if (owner == null) return;
        var lineage = CreatureCareLineage.get(release.actor.serverLevel());
        for (var claim : NativeFoodSources.claims(spent)) {
            var state = lineage.mouse(claim.batch());
            boolean preserved = state != null && state.equals(CreatureCareLineage.nativeState(release.released));
            if (physical.consume(owner, spent, "creature_mouse_pot", 1)) {
                lineage.usedMouse(claim.batch());
                if (preserved && owner.equals(release.actor.getUUID())) CreatureCareProof.emit(release.actor, "v3_fauna_pot_and_present");
                break;
            }
        }
    }
}
