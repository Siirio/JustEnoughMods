package com.justenoughmods.achievementguide.mixin;

import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Reconciles the native lock before FTB sends its post-definition team snapshot. */
@Mixin(targets = "dev.ftb.mods.ftbquests.net.RequestTeamDataMessage", remap = false)
public abstract class FtbTeamDataRequestMixin {
    @Redirect(method = "handle", at = @At(value = "INVOKE",
            target = "Ldev/ftb/mods/ftbquests/quest/TeamData;get(Lnet/minecraft/world/entity/player/Player;)Ldev/ftb/mods/ftbquests/quest/TeamData;"),
            remap = false)
    private TeamData jem$unlockBeforeNativeSnapshot(Player player) {
        TeamData data = TeamData.get(player);
        if (data != null && data.setLocked(false)) {
            data.saveIfChanged();
        }
        return data;
    }
}
