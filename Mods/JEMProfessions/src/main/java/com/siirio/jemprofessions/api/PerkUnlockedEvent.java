package com.siirio.jemprofessions.api;

import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public final class PerkUnlockedEvent extends Event {
    private final ServerPlayer player;
    private final ProfessionId profession;
    private final String nodeId;
    private final int level;
    public PerkUnlockedEvent(ServerPlayer player, ProfessionId profession, String nodeId, int level) {
        this.player = player; this.profession = profession; this.nodeId = nodeId; this.level = level;
    }
    public ServerPlayer player() { return player; }
    public ProfessionId profession() { return profession; }
    public String nodeId() { return nodeId; }
    public int level() { return level; }
}
