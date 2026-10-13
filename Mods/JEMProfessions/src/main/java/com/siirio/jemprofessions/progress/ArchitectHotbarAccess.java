package com.siirio.jemprofessions.progress;

import com.siirio.jemprofessions.api.ProfessionApi;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

final class ArchitectHotbarAccess {
    private static final ResourceLocation TINYINV_HOTBAR = new ResourceLocation("tinyinv", "hotbar_slots");
    private static final UUID MODIFIER_ID = UUID.fromString("953cabee-0892-48e8-8bb7-a42e422f59b0");
    private static final AttributeModifier FOUR_SLOTS = new AttributeModifier(MODIFIER_ID, "JEM Architect quick slots", 4.0D, AttributeModifier.Operation.ADDITION);

    private ArchitectHotbarAccess() { }

    static void reconcile(ServerPlayer player) {
        Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(TINYINV_HOTBAR);
        if (attribute == null) return;
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return;
        boolean eligible = ProfessionApi.level(player, ProfessionId.ARCHITECT) >= 20;
        AttributeModifier current = instance.getModifier(MODIFIER_ID);
        if (eligible && current == null) instance.addTransientModifier(FOUR_SLOTS);
        else if (!eligible && current != null) instance.removeModifier(MODIFIER_ID);
    }
}
