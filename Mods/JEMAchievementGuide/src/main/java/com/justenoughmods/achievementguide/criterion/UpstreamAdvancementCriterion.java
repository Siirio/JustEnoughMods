package com.justenoughmods.achievementguide.criterion;

import com.google.gson.JsonObject;
import com.justenoughmods.achievementguide.JemAdvancementGuide;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public final class UpstreamAdvancementCriterion extends SimpleCriterionTrigger<UpstreamAdvancementCriterion.Instance> {
    private static final ResourceLocation ID = new ResourceLocation(JemAdvancementGuide.MOD_ID, "upstream_advancement");

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(player, new ResourceLocation(GsonHelper.getAsString(json, "source")));
    }

    public void trigger(ServerPlayer player, ResourceLocation source) {
        trigger(player, instance -> instance.source.equals(source));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final ResourceLocation source;

        Instance(ContextAwarePredicate player, ResourceLocation source) {
            super(ID, player);
            this.source = source;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("source", source.toString());
            return json;
        }
    }
}
