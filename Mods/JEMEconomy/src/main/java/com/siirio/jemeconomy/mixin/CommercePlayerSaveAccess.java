package com.siirio.jemeconomy.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Narrow native save path, applied only with the sandbox marker. */
@Mixin(PlayerList.class)
public interface CommercePlayerSaveAccess {
    @Invoker("save") void jem$commerceSave(ServerPlayer player);
}
