package com.siirio.jemserver.smp.compat;

import com.siirio.jemserver.smp.events.BossDebuffController;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.entity.eventlistener.PlayerEventListener;

public final class EpicFightDebuffBridge {
    private static final String NIGHTFALL_NAMESPACE="efn";
    private static final UUID BASIC_ATTACK_LISTENER=listenerId("basic_attack");
    private static final UUID SKILL_CAST_LISTENER=listenerId("skill_cast");

    public static void register(ServerPlayer player) {
        var patch=EpicFightCapabilities.getServerPlayerPatch(player);
        if(patch==null) return;
        var listeners=patch.getEventListener();
        listeners.removeListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT,BASIC_ATTACK_LISTENER);
        listeners.removeListener(PlayerEventListener.EventType.SKILL_CAST_EVENT,SKILL_CAST_LISTENER);
        listeners.addEventListener(PlayerEventListener.EventType.BASIC_ATTACK_EVENT,BASIC_ATTACK_LISTENER,event->{
            ServerPlayer actor=event.getPlayerPatch().getOriginal();
            if(BossDebuffController.nightfallBlocked(actor)&&heldNamespace(actor).equals(NIGHTFALL_NAMESPACE)) {
                event.setCanceled(true);
                BossDebuffController.explainBlocked(actor,"nightfall");
            } else if(BossDebuffController.epicMovesetBlocked(actor)) {
                event.setCanceled(true);
                BossDebuffController.explainBlocked(actor,"epic");
            }
        });
        listeners.addEventListener(PlayerEventListener.EventType.SKILL_CAST_EVENT,SKILL_CAST_LISTENER,event->{
            ResourceLocation skill=event.getSkillContainer().getSkill()==null?null:event.getSkillContainer().getSkill().getRegistryName();
            ServerPlayer actor=(ServerPlayer)event.getPlayerPatch().getOriginal();
            boolean nightfall=skill!=null&&skill.getNamespace().equals(NIGHTFALL_NAMESPACE)&&BossDebuffController.nightfallBlocked(actor);
            boolean epic=BossDebuffController.epicMovesetBlocked(actor);
            if(!nightfall&&!epic) return;
            event.setCanceled(true);
            event.setSkillExecutable(false);
            event.setStateExecutable(false);
            BossDebuffController.explainBlocked(actor,nightfall?"nightfall":"epic");
        });
    }

    private static String heldNamespace(ServerPlayer player) {
        return BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).getNamespace();
    }

    private static UUID listenerId(String name) {
        return UUID.nameUUIDFromBytes(("jem_server:blood_moon_"+name).getBytes(StandardCharsets.UTF_8));
    }

    private EpicFightDebuffBridge() {}
}
