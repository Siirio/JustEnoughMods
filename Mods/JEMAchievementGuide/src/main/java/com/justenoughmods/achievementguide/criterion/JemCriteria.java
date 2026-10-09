package com.justenoughmods.achievementguide.criterion;

import com.justenoughmods.achievementguide.JemAdvancementGuide;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Map;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

public final class JemCriteria {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UpstreamAdvancementCriterion UPSTREAM_ADVANCEMENT = new UpstreamAdvancementCriterion();
    private static final Map<ResourceLocation, NodeCriterion> NODES = new LinkedHashMap<>();
    private static boolean registered;

    private JemCriteria() {
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(JemCriteria::register);
    }

    private static synchronized void register() {
        if (registered) {
            return;
        }
        CriteriaTriggers.register(UPSTREAM_ADVANCEMENT);
        registered = true;
    }

    public static synchronized void registerNodes(Collection<String> ids) {
        for (String value : ids) {
            ResourceLocation id = new ResourceLocation(value);
            if (NODES.containsKey(id)) {
                continue;
            }
            NodeCriterion criterion = new NodeCriterion(id);
            CriteriaTriggers.register(criterion);
            NODES.put(id, criterion);
        }
        LOGGER.info("[JEM Advancements] Registered {} node criteria and the upstream advancement criterion", NODES.size());
    }

    public static NodeCriterion node(ResourceLocation criterionId) {
        NodeCriterion criterion = NODES.get(criterionId);
        if (criterion == null) {
            throw new IllegalArgumentException("Unregistered JEM criterion " + criterionId);
        }
        return criterion;
    }

    public static void fire(ServerPlayer player, ResourceLocation advancementId) {
        ResourceLocation criterionId = new ResourceLocation(
                JemAdvancementGuide.MOD_ID,
                "node/" + advancementId.getNamespace() + "/" + advancementId.getPath()
        );
        node(criterionId).trigger(player);
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UPSTREAM_ADVANCEMENT.trigger(player, event.getAdvancement().getId());
        }
    }

}
