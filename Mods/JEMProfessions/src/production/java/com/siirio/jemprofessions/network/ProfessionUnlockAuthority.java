package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import net.minecraft.server.level.ServerPlayer;

/** Existing production unlock mechanism; sandbox availability rules cannot leak into it. */
final class ProfessionUnlockAuthority {
    private ProfessionUnlockAuthority() { }
    static void unlock(ServerPlayer player,ProfessionId profession,String node) {
        ProfessionApi.category(profession).flatMap(value->value.getSkill(node)).ifPresent(skill->skill.unlock(player));
    }
}
