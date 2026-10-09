package com.justenoughmods.achievementguide.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NodeCriterion extends SimpleCriterionTrigger<NodeCriterion.Instance> {
    private final ResourceLocation id;

    NodeCriterion(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player, DeserializationContext context) {
        return new Instance(id, player);
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        Instance(ResourceLocation id, ContextAwarePredicate player) {
            super(id, player);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            return super.serializeToJson(context);
        }
    }
}
