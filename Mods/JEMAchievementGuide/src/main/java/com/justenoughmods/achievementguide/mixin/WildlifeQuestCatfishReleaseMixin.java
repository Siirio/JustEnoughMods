package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestProof;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRun;
import com.github.alexthe666.alexsmobs.entity.EntityCatfish;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityCatfish", remap = false)
public abstract class WildlifeQuestCatfishReleaseMixin {
    @Unique private WildlifeQuestRun jem$release;
    @Unique private CompoundTag jem$contained = new CompoundTag();
    @Unique private String jem$type = "";
    @Inject(method = "m_6071_", at = @At("HEAD"))
    private void jem$before(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var self = (EntityCatfish)(Object)this; var actor = WildlifeQuestProof.real(player); jem$release = null;
        if (actor != null && player.getItemInHand(hand).is(Items.SEA_PICKLE) && self.hasSwallowedEntity()
                && WildlifeQuestProof.matches(self, "bucket_native_actor", actor.getUUID()) && WildlifeQuestProof.data(self).getBoolean("catfish_valid_capture")) {
            jem$contained = self.getSwallowedData().copy(); jem$type = self.getSwallowedEntityType();
            jem$release = WildlifeQuestRun.begin(self, actor, player.getItemInHand(hand));
        }
    }
    @Inject(method = "m_6071_", at = @At("RETURN"))
    private void jem$after(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        var run = jem$release; jem$release = null; WildlifeQuestRun.end(run); var self = (EntityCatfish)(Object)this;
        if (run != null && callback.getReturnValue().consumesAction() && !self.hasSwallowedEntity()) {
            for (var entity : run.created) if (entity instanceof LivingEntity alive && alive.isAlive() && WildlifeQuestProof.type(entity).equals(jem$type)
                    && run.actor.serverLevel().getEntity(entity.getUUID()) == entity) {
                CompoundTag actual = new CompoundTag(); entity.saveWithoutId(actual); boolean preserved = true;
                // The native spit explicitly resets health to a positive fraction. Every other stored native field must survive.
                for (String key : jem$contained.getAllKeys()) if (!key.equals("Health") && !jem$contained.get(key).equals(actual.get(key))) preserved = false;
                if (preserved && WildlifeQuestProof.data(self).hasUUID("catfish_swallowed_source")) {
                    WildlifeQuestProof.data(entity).putUUID("catfish_native_source", WildlifeQuestProof.data(self).getUUID("catfish_swallowed_source"));
                    WildlifeQuestProof.emit(run.actor, "v3_fauna_creature_transport");
                }
            }
        }
        jem$contained = new CompoundTag(); jem$type = "";
    }
}
