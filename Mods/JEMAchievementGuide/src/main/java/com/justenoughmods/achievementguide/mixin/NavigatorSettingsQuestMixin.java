package com.justenoughmods.achievementguide.mixin;

import com.justenoughmods.achievementguide.integration.create.CreateNavigatorProofs;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.network.packets.pain.SaveUserSettingsPacketData;
import de.mrjulsen.mcdragonlib.network.NetworkPacketContext;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "de.mrjulsen.crn.network.packets.pain.SaveUserSettingsPacketData", remap = false)
public abstract class NavigatorSettingsQuestMixin {
    @Redirect(method = "handle", at = @At(value = "INVOKE", target = "Lde/mrjulsen/crn/data/UserSettings;save()V"), remap = false)
    private static void jem$nativeConstraintCommit(UserSettings settings, SaveUserSettingsPacketData packet, NetworkPacketContext context) {
        ServerPlayer actor = context.getPlayer() instanceof ServerPlayer player && player.getUUID().equals(settings.getOwnerId()) ? player : null;
        var before = CreateNavigatorProofs.eligible(actor) ? CreateNavigatorProofs.constraint(UserSettings.getSettingsFor(actor.getUUID(), true)) : null;
        settings.save();
        CreateNavigatorProofs.saved(actor, settings, before);
    }
}
