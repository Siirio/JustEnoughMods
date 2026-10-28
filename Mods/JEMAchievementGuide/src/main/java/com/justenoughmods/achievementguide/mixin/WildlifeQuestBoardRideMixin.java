package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.food.NativeFoodSources;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestRides;
import com.justenoughmods.achievementguide.integration.wildlife.WildlifeQuestTools;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityStraddleboard", remap = false)
public abstract class WildlifeQuestBoardRideMixin implements WildlifeQuestRides.NativeRide {
    @Shadow public abstract ItemStack getItemStack();
    @Shadow private boolean isOnLava() { throw new AssertionError(); }
    @Override public String jem$wildlifeRideAction(ServerPlayer actor) {
        if (!isOnLava()) return "";
        return actor.getUUID().equals(NativeFoodSources.get(actor.serverLevel()).owner(getItemStack(), "wildlife_board")) ? "v3_fauna_lava_nursery_board" : "";
    }
    @Override public boolean jem$wildlifeRideFinished(ServerPlayer actor) {
        return WildlifeQuestTools.consume(actor, "wildlife_board", WildlifeQuestTools.get(actor.serverLevel()).origin(getItemStack()));
    }
}
