package com.justenoughmods.achievementguide.integration.beach;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/** Two short-lived observations on one authenticated native player connection. */
public final class BeachTraversalProof {
    public interface Access {
        BeachTraversalProof jem$nativeBeachTraversal();
    }

    private static final ResourceLocation CROCS = new ResourceLocation("beachparty", "crocs");
    private static final ResourceLocation OCEAN_WALK = new ResourceLocation("beachparty", "ocean_walk");
    private record Observation(int entity, ResourceKey<Level> dimension, long tick, Vec3 from, Vec3 to) {}
    private Observation support;
    private Observation movement;

    public void clear() { support = null; movement = null; }

    public void expire(ServerPlayer player) {
        long now = player.serverLevel().getGameTime();
        if (!current(support, player, now)) support = null;
        if (!current(movement, player, now)) movement = null;
    }

    public void supported(ServerPlayer player) {
        expire(player);
        support = eligible(player) ? observation(player, player.position(), player.position()) : null;
        complete(player);
    }

    public void acceptedMovement(ServerPlayer player, Vec3 before) {
        expire(player);
        Vec3 after = player.position();
        movement = eligible(player) && finite(before) && finite(after)
                && (after.x != before.x || after.z != before.z)
                ? observation(player, before, after) : null;
        complete(player);
    }

    private void complete(ServerPlayer player) {
        if (support == null || movement == null || !eligible(player)) return;
        // Either server ordering is valid, but the native support must touch this exact accepted move.
        if (!support.from.equals(movement.from) && !support.from.equals(movement.to)) return;
        clear();
        QuestActionTrigger.INSTANCE.emit(player, "v3_food_beach_gear");
    }

    private static Observation observation(ServerPlayer player, Vec3 from, Vec3 to) {
        return new Observation(player.getId(), player.level().dimension(), player.serverLevel().getGameTime(), from, to);
    }

    private static boolean current(Observation receipt, ServerPlayer player, long now) {
        return receipt != null && receipt.entity == player.getId() && receipt.dimension.equals(player.level().dimension())
                && now >= receipt.tick && now - receipt.tick <= 1;
    }

    private static boolean finite(Vec3 vector) {
        return vector != null && Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static boolean eligible(ServerPlayer player) {
        if (!BeachGoalRun.realActor(player) || !player.isSprinting() || player.isInWater()
                || player.isPassenger() || player.isSleeping() || player.isFallFlying()
                || player.isChangingDimension() || player.noPhysics || player.getAbilities().flying) return false;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(OCEAN_WALK);
        if (effect == null || !player.hasEffect(effect)) return false;
        return CuriosApi.getCuriosInventory(player).resolve().flatMap(inventory -> inventory.getStacksHandler("feet"))
                .map(feet -> {
                    var stacks = feet.getStacks();
                    int slots = stacks.getSlots();
                    if (slots < 1 || slots > 64) return false;
                    for (int slot = 0; slot < slots; slot++) {
                        var stack = stacks.getStackInSlot(slot);
                        if (!stack.isEmpty() && CROCS.equals(ForgeRegistries.ITEMS.getKey(stack.getItem()))) return true;
                    }
                    return false;
                }).orElse(false);
    }
}
