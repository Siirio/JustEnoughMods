package com.justenoughmods.achievementguide.mixin;
import com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale;
import com.justenoughmods.achievementguide.integration.wildlife.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCachalotWhale", remap = false)
public abstract class WildlifeQuestWhaleRescueMixin {
    @Unique private boolean jem$beached;
    @Unique private float jem$health;
    @Inject(method = "setBeached", at = @At("HEAD"))
    private void jem$before(boolean state, CallbackInfo callback) { jem$beached = ((EntityCachalotWhale)(Object)this).isBeached(); }
    @Inject(method = "setBeached", at = @At("RETURN"))
    private void jem$rescued(boolean state, CallbackInfo callback) {
        EntityCachalotWhale self = (EntityCachalotWhale)(Object)this; var actor = WildlifeQuestProof.actor(self, "rescue_actor");
        if (actor == null || !jem$beached || state || self.isBeached() || !self.isEyeInFluid(FluidTags.WATER) || !self.isAlive()) return;
        var proof = WildlifeQuestProof.data(self); BlockPos changed = BlockPos.of(proof.getLong("rescue_change"));
        if (self.level().getGameTime() - proof.getLong("rescue_time") <= 300 && !WildlifeQuestWhales.attacked(self, actor.getUUID())
                && self.getBoundingBox().inflate(1).intersects(new AABB(changed)) && NbtUtils.writeBlockState(self.level().getBlockState(changed)).equals(proof.getCompound("rescue_state"))) {
            WildlifeQuestProof.emit(actor, "v3_fauna_whale_rescue"); proof.remove("rescue_actor");
        }
    }
}
