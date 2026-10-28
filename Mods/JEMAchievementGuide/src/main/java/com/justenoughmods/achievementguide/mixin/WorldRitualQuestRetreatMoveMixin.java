package com.justenoughmods.achievementguide.mixin;
import com.justenoughmods.achievementguide.integration.worldritual.WorldRitualQuestStun;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class WorldRitualQuestRetreatMoveMixin {
    @Shadow public ServerPlayer player;
    @Unique private Vec3 jem$ritualBefore;
    @Inject(method="handleMovePlayer",at=@At("HEAD"))private void jem$before(ServerboundMovePlayerPacket packet,CallbackInfo ci){jem$ritualBefore=player.position();}
    @Inject(method="handleMovePlayer",at=@At("RETURN"))private void jem$after(ServerboundMovePlayerPacket packet,CallbackInfo ci){if(jem$ritualBefore!=null)WorldRitualQuestStun.moved(player,jem$ritualBefore);jem$ritualBefore=null;}
}
