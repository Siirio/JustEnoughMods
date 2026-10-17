package com.siirio.jemprofessions.network;

import com.siirio.jemprofessions.api.ProfessionApi;
import com.siirio.jemprofessions.progress.ProfessionId;
import com.siirio.jemprofessions.sandbox.SandboxProfessionPolicy;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Skill;

/** Validate at the authoritative packet boundary, including crafted client requests. */
final class ProfessionUnlockAuthority {
    private ProfessionUnlockAuthority() { }
    static void unlock(ServerPlayer player,ProfessionId profession,String node) {
        if(player.server.isDedicatedServer()||!SandboxProfessionPolicy.available(node))return;
        var definition=com.siirio.jemprofessions.tree.ProfessionCatalog.node(profession,node);
        if(definition==null||ProfessionApi.level(player,profession)<definition.level()
                ||!definition.parentId().isBlank()&&!ProfessionApi.enabled(player,profession,definition.parentId()))return;
        ProfessionApi.category(profession).ifPresent(category->{
            if(category.getPointsLeft(player)<=0)return;
            category.getSkill(node).filter(skill->skill.getState(player)==Skill.State.AFFORDABLE)
                    .ifPresent(skill->skill.unlock(player));
        });
    }
}
