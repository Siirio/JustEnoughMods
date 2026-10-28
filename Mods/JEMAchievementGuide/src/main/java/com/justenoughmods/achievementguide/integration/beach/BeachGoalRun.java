package com.justenoughmods.achievementguide.integration.beach;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.util.FakePlayer;

/** One native goal tick, without repeating its entity query or shape evaluation. */
public final class BeachGoalRun {
    public interface Access {
        BeachGoalRun jem$nativeGoalRun();
    }

    private Entity iterated;
    private Entity intersected;
    private UUID actor;
    private boolean armed;
    private boolean inserted;

    public void clear() {
        iterated = null;
        intersected = null;
        actor = null;
        armed = false;
        inserted = false;
    }

    public void iterated(Entity ball) { iterated = ball; }

    public void shapeResult(boolean intersects) {
        if (intersects) intersected = iterated;
        iterated = null;
    }

    public void arm() {
        armed = intersected instanceof BeachBallActor && !intersected.isRemoved();
        actor = armed ? ((BeachBallActor)intersected).jem$lastNativeBallActor() : null;
        inserted = false;
    }

    public void inserted(boolean accepted) {
        if (armed && accepted) inserted = true;
    }

    public void completed(ServerLevel level, boolean nativeLatch) {
        UUID credited = armed && inserted && nativeLatch && intersected != null
                && !intersected.isRemoved() && intersected.level() == level ? actor : null;
        clear();
        if (credited == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(credited);
        if (realActor(player) && player.level() == level) {
            QuestActionTrigger.INSTANCE.emit(player, "v3_food_beach_goal");
        }
    }

    public static boolean realActor(ServerPlayer player) {
        return player != null && !(player instanceof FakePlayer) && player.isAlive()
                && !player.isSpectator() && !player.getAbilities().instabuild;
    }
}
