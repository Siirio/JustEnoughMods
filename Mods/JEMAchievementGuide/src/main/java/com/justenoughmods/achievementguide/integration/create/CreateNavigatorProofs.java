package com.justenoughmods.achievementguide.integration.create;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import de.mrjulsen.crn.data.StationTag;
import de.mrjulsen.crn.data.UserSettings;
import de.mrjulsen.crn.data.navigation.Route;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;

/** Native committed route settings and authoritative native itinerary, no client-reported proof. */
public final class CreateNavigatorProofs {
    private CreateNavigatorProofs() {}
    public static boolean eligible(ServerPlayer actor) {
        return actor != null && !(actor instanceof FakePlayer) && !actor.isSpectator() && !actor.getAbilities().instabuild;
    }
    public static CompoundTag constraint(UserSettings settings) {
        var categories = settings.navigationExcludedTrainCategories.getValue();
        if (categories.size() > 128) return null;
        CompoundTag tag = new CompoundTag();
        tag.putInt("transfer", settings.navigationTransferTime.getValue());
        ListTag excluded = new ListTag(); categories.stream().map(Object::toString).sorted().forEach(id -> excluded.add(StringTag.valueOf(id)));
        tag.put("categories", excluded); return tag;
    }
    private static boolean active(CompoundTag constraint) {
        return constraint != null && constraint.getInt("transfer") >= 0
                && (constraint.getInt("transfer") != 1000 || !constraint.getList("categories", 8).isEmpty());
    }
    public static void saved(ServerPlayer actor, UserSettings settings, CompoundTag before) {
        if (!eligible(actor) || before == null || !actor.getUUID().equals(settings.getOwnerId())
                || UserSettings.getSettingsFor(actor.getUUID(), true) != settings) return;
        CompoundTag after = constraint(settings);
        if (!active(after) || before.equals(after)) return;
        actor.serverLevel().getServer().execute(() -> {
            if (after.equals(constraint(UserSettings.getSettingsFor(actor.getUUID(), true))))
                QuestActionTrigger.INSTANCE.emit(actor, "create_navigator_constraint");
        });
    }
    public static void queried(ServerPlayer actor, StationTag start, StationTag end, List<Route> routes, CompoundTag settings) {
        if (!eligible(actor) || !active(settings) || start.getId().equals(end.getId()) || routes.isEmpty() || routes.size() > 64) return;
        boolean valid = false;
        for (Route route : routes) {
            var parts = ((NavigatorRouteAccess)(Object)route).jem$parts();
            if (parts.isEmpty() || parts.size() > 32 || parts.stream().anyMatch(part -> part.isEmpty() || part.isCancelled())) continue;
            var first = route.getStart().getScheduledStationTag(); var last = route.getEnd().getScheduledStationTag();
            if (start.getId().equals(first.tagId()) && end.getId().equals(last.tagId()) && !first.stationName().equals(last.stationName())) { valid = true; break; }
        }
        if (!valid) return;
        actor.serverLevel().getServer().execute(() -> {
            if (!settings.equals(constraint(UserSettings.getSettingsFor(actor.getUUID(), true)))) return;
            var advancement = actor.serverLevel().getServer().getAdvancements().getAdvancement(new ResourceLocation("jem_advancements", "quest_v3/v3_eng_route_planner"));
            if (advancement == null) return;
            var stage = actor.getAdvancements().getOrStartProgress(advancement).getCriterion("constraint");
            if (stage != null && stage.isDone()) QuestActionTrigger.INSTANCE.emit(actor, "create_navigator_route");
        });
    }
}
