package com.justenoughmods.achievementguide.mixin;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbteams.data.ClientTeamManagerImpl;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Uses the UUID issued by the connected server for the native FTB Teams self lookup. */
@Mixin(value = ClientTeamManagerImpl.class, remap = false)
public abstract class FtbClientIdentityMixin {
    @Redirect(method = "initSelfDetails(Ljava/util/UUID;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/GameProfile;getId()Ljava/util/UUID;"),
            remap = false)
    private UUID jem$connectedPlayerId(GameProfile profile) {
        var player = Minecraft.getInstance().player;
        return player == null ? profile.getId() : player.getUUID();
    }
}
