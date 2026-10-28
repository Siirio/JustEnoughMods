package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.home.HomeInteractionProof;
import immersive_melodies.item.InstrumentItem;
import immersive_melodies.resources.ServerMelodyManager;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstrumentItem.class, remap = false)
public abstract class HomeMusicStartQuestMixin {
    @Unique private final ThreadLocal<Deque<CompoundTag>> jem$beforeMusic = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "play(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
    private void jem$before(ItemStack stack, ResourceLocation melody, Level level, Entity entity, CallbackInfo callback) {
        if (jem$beforeMusic.get().size() >= 16) jem$beforeMusic.get().clear();
        jem$beforeMusic.get().push(stack.hasTag() ? stack.getTag().copy() : new CompoundTag());
    }

    @Inject(method = "play(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)V", at = @At("RETURN"))
    private void jem$after(ItemStack stack, ResourceLocation melody, Level level, Entity entity, CallbackInfo callback) {
        CompoundTag before = jem$beforeMusic.get().poll();
        if (before == null || !(entity instanceof ServerPlayer player) || HomeInteractionProof.actor(player) == null
                || player.level() != level || player.getMainHandItem() != stack && player.getOffhandItem() != stack
                || !melody.getNamespace().equals("immersive_melodies") || !stack.hasTag()) return;
        var nativeMelody = ServerMelodyManager.getDatapackMelodies().get(melody);
        CompoundTag after = stack.getTag();
        if (nativeMelody != null && nativeMelody.get().getTracks().stream().anyMatch(track -> !track.getNotes().isEmpty())
                && after.getBoolean("playing") && after.getString("melody").equals(melody.toString())
                && after.getLong("start_time") == level.getGameTime() && !before.equals(after))
            HomeInteractionProof.emit(player, "v3_home_play_music");
    }
}
