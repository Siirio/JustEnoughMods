package com.justenoughmods.achievementguide.mixin;

import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies the shared real-player admission only to the five explicitly reviewed native V4 resources. */
@Mixin(SimpleCriterionTrigger.class)
public abstract class NativeQuestActorMixin {
    @Unique private static final Set<String> jem$nativeQuestPaths = Set.of(
            "quest_v2/biomes/scenic_detour", "quest_v2/biomes/stranger_nether", "quest_v2/biomes/world_under_world",
            "quest_v3/v3_home_nether_flesh", "quest_v3/v3_world_kaolin_knock");

    @Redirect(method = "trigger", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/advancements/CriterionTrigger$Listener;run(Lnet/minecraft/server/PlayerAdvancements;)V"))
    private void jem$realNativeActor(CriterionTrigger.Listener<?> listener, PlayerAdvancements progress,
                                     ServerPlayer player, Predicate<?> predicate) {
        var id = ((NativeQuestListenerAccess) listener).jem$questAdvancement().getId();
        if (id.getNamespace().equals("jem_advancements") && jem$nativeQuestPaths.contains(id.getPath())
                && (player instanceof FakePlayer || player.isSpectator() || player.getAbilities().instabuild)) return;
        listener.run(progress);
    }
}
