package com.justenoughmods.achievementguide.advancement;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.util.FakePlayer;

/** Native criterion only. Owning systems emit after their real successful action. */
public final class QuestActionTrigger extends SimpleCriterionTrigger<QuestActionTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("jem_advancements", "quest_action");
    public static final QuestActionTrigger INSTANCE = new QuestActionTrigger();

    private QuestActionTrigger() {}

    /** Call once from the existing Guide criterion bootstrap, before resources load. */
    public static void register() {
        CriteriaTriggers.register(INSTANCE);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                      DeserializationContext context) {
        return new Instance(player, GsonHelper.getAsString(json, "action"));
    }

    public void emit(ServerPlayer player, String action) {
        if (player == null || player instanceof FakePlayer || player.isSpectator()
                || player.getAbilities().instabuild) {
            return;
        }
        if (!player.serverLevel().getServer().isSameThread()) {
            throw new IllegalStateException("Quest success must be emitted on the logical server thread");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Quest success action must be named");
        }
        if (player.server.getPlayerList().getPlayer(player.getUUID()) != player || player.connection == null
                || !player.connection.isAcceptingMessages()
                || !com.justenoughmods.achievementguide.integration.travel.TravelProof.authenticated(player)) {
            return;
        }
        trigger(player, instance -> instance.action.equals(action));
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final String action;

        private Instance(ContextAwarePredicate player, String action) {
            super(ID, player);
            if (action.isBlank()) {
                throw new IllegalArgumentException("Quest success action must be named");
            }
            this.action = action;
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("action", action);
            return json;
        }
    }
}
