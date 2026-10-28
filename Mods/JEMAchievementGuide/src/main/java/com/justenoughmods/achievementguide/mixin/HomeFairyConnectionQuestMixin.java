package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeFairyLineage;
import com.justenoughmods.achievementguide.integration.home.HomeFairyProof;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import me.paulf.fairylights.server.connection.Connection;
import me.paulf.fairylights.server.fastener.Fastener;
import me.paulf.fairylights.server.feature.FeatureType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Connection.class, remap = false)
public abstract class HomeFairyConnectionQuestMixin implements HomeFairyLineage {
    @Shadow protected float slack;
    @Unique private UUID jem$homeCreator;
    @Unique private final ThreadLocal<Deque<Float>> jem$slackBefore = ThreadLocal.withInitial(ArrayDeque::new);
    @Override public UUID jem$homeCreator() { return jem$homeCreator; }
    @Override public void jem$homeCreator(UUID player) { jem$homeCreator = player; }

    @Inject(method = "reconnect", at = @At("RETURN"))
    private void jem$connected(Fastener<?> destination, CallbackInfoReturnable<Boolean> callback) {
        HomeFairyProof.connected((Connection)(Object)this, callback.getReturnValue());
    }
    @Inject(method = "interact", at = @At("HEAD"))
    private void jem$before(Player player, Vec3 hit, FeatureType type, int feature, ItemStack held, InteractionHand hand, CallbackInfoReturnable<Boolean> callback) {
        if (jem$slackBefore.get().size() >= 16) jem$slackBefore.get().clear();
        jem$slackBefore.get().push(slack);
    }
    @Inject(method = "interact", at = @At("RETURN"))
    private void jem$after(Player player, Vec3 hit, FeatureType type, int feature, ItemStack held, InteractionHand hand, CallbackInfoReturnable<Boolean> callback) {
        Float before = jem$slackBefore.get().poll();
        if (before != null) HomeFairyProof.slackened((Connection)(Object)this, player, before, slack, callback.getReturnValue());
    }
    @Inject(method = "serialize()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void jem$save(CallbackInfoReturnable<CompoundTag> callback) {
        if (jem$homeCreator != null) callback.getReturnValue().putUUID("jem_home_creator", jem$homeCreator);
    }
    @Inject(method = "deserialize(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void jem$load(CompoundTag tag, CallbackInfo callback) {
        jem$homeCreator = tag.hasUUID("jem_home_creator") ? tag.getUUID("jem_home_creator") : null;
    }
}
