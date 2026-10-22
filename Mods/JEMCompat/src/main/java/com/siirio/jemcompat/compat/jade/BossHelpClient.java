package com.siirio.jemcompat.compat.jade;

import com.justenoughmods.achievementguide.client.handbook.HandbookNavigation;
import net.minecraft.network.chat.Component;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.ITooltip;

/** Client-only binding text stays out of server snapshot production. */
final class BossHelpClient {
    private BossHelpClient() { }

    static void addRoute(ITooltip tooltip, EntityAccessor accessor) {
        if (HandbookNavigation.entryForEntity(accessor.getEntity()) != null) {
            tooltip.add(Component.translatable("jem_advancements.handbook.entity_details",
                    HandbookNavigation.HELP_KEY.getTranslatedKeyMessage()));
        }
    }
}
