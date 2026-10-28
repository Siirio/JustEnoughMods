package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.github.alexthe666.alexsmobs.entity.EntityTendonSegment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityTendonSegment", remap = false)
public abstract class WildlifeQuestTendonChainMixin {
    @Redirect(method = "m_8119_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean jem$chain(Entity target, DamageSource source, float damage) {
        float before = target instanceof LivingEntity living ? living.getHealth() : 0; boolean accepted = target.hurt(source, damage);
        EntityTendonSegment self = (EntityTendonSegment)(Object)this;
        var actor = self.getCreatorEntity() instanceof Player player ? WildlifeQuestProof.real(player) : null;
        if (accepted && target instanceof LivingEntity living && living.getHealth() < before && WildlifeQuestProof.hostile(living, actor)) {
            EntityTendonSegment root = self;
            for (int i = 0; i < 5 && root.getFromEntity() instanceof EntityTendonSegment parent; i++) root = parent;
            if (root.getFromEntity() instanceof EntityTendonSegment || !actor.getUUID().equals(root.getCreatorEntityUUID())) return accepted;
            var proof = WildlifeQuestProof.data(root); ListTag hit = proof.getList("hostile_hits", 10); boolean known = false;
            for (int i = 0; i < hit.size(); i++) if (hit.getCompound(i).hasUUID("target") && hit.getCompound(i).getUUID("target").equals(target.getUUID())) known = true;
            if (!known && hit.size() < 5) { CompoundTag row = new CompoundTag(); row.putUUID("target", target.getUUID()); hit.add(row); proof.put("hostile_hits", hit); }
            if (hit.size() >= 2) WildlifeQuestProof.emit(actor, "v3_fauna_wild_gear_tendon");
        }
        return accepted;
    }
}
