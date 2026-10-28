package com.justenoughmods.achievementguide.integration.food;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import com.li64.tide.registries.entities.misc.fishing.TideFishingHook;
import com.li64.tide.registries.entities.misc.fishing.HookAccessor;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** Native catch scopes retain the native selected rod, mappings and accepted spawned object. */
public final class TideCatchProof {
    public static final String CRATE = "JemQuestNativeCrateV4";
    private static final Set<String> HOOKS = Set.of("tide:fiery_hook", "tide:permafrost_hook", "tide:twilight_hook");
    private static final Set<String> CRATES = Set.of("tide:wooden_crate", "tide:obsidian_crate", "tide:purpur_crate");
    private record Catch(TideFishingHook hook, FishingHook accessor, ServerPlayer actor, boolean fish, boolean crate, boolean contextual) {}
    private static final ThreadLocal<Catch> CATCH = new ThreadLocal<>();
    private TideCatchProof() {}

    public static void begin(TideFishingHook hook, Player player, int nibble) {
        CATCH.remove();
        ServerPlayer actor = RemainingFoodActors.real(player);
        if (actor == null || hook.getPlayerOwner() != actor || HookAccessor.getHook(actor) != hook || nibble <= 0) return;
        CATCH.set(new Catch(hook, actor.fishing, actor, hook.getCatchType() == TideFishingHook.CatchType.FISH,
                hook.getCatchType() == TideFishingHook.CatchType.CRATE,
                HOOKS.contains(RemainingFoodActors.item(hook.getHook()))));
    }
    public static void end() { CATCH.remove(); }
    public static void converted(FishingHook hook, ServerPlayer actor) {
        Catch run = CATCH.get();
        if (run != null && run.actor == actor && run.accessor == hook && actor.fishing == hook
                && HookAccessor.getHook(actor) == run.hook && hook.getPlayerOwner() == actor && run.fish && run.contextual) {
            QuestActionTrigger.INSTANCE.emit(actor, "v3_food_context_tackle");
        }
    }

    public static void item(Entity entity, boolean accepted) {
        Catch run = CATCH.get();
        if (!accepted || run == null || !run.fish || !run.contextual || !(entity instanceof ItemEntity drop)
                || drop.getItem().isEmpty() || entity.level() != run.actor.level()) return;
        if (run.hook.getHookedItems().stream().anyMatch(stack -> stack == drop.getItem())) {
            QuestActionTrigger.INSTANCE.emit(run.actor, "v3_food_context_tackle");
        }
    }

    public static void crate(Entity entity, boolean accepted) {
        Catch run = CATCH.get();
        if (!accepted || run == null || !run.crate || !(entity instanceof FallingBlockEntity falling)
                || entity.level() != run.actor.level()
                || !CRATES.contains(String.valueOf(ForgeRegistries.BLOCKS.getKey(falling.getBlockState().getBlock())))) return;
        CompoundTag proof = new CompoundTag();
        proof.putUUID("actor", run.actor.getUUID());
        entity.getPersistentData().put(CRATE, proof);
    }

    public static void landed(Entity crate, BlockEntity container, boolean received) {
        CompoundTag proof = crate.getPersistentData().getCompound(CRATE);
        if (!received || !proof.hasUUID("actor") || container.getLevel() != crate.level()) return;
        container.getPersistentData().put(CRATE, proof.copy());
        container.setChanged();
        ServerPlayer actor = crate.getServer().getPlayerList().getPlayer(proof.getUUID("actor"));
        if (RemainingFoodActors.real(actor) != null && actor.level() == crate.level()) {
            QuestActionTrigger.INSTANCE.emit(actor, "food_crate_caught");
        }
        crate.getPersistentData().remove(CRATE);
    }

    public static void opened(ServerPlayer actor, BlockEntity crate) {
        CompoundTag proof = crate.getPersistentData().getCompound(CRATE);
        if (RemainingFoodActors.real(actor) == null || !proof.hasUUID("actor") || !actor.getUUID().equals(proof.getUUID("actor"))) return;
        crate.getPersistentData().remove(CRATE);
        crate.setChanged();
        QuestActionTrigger.INSTANCE.emit(actor, "food_crate_caught");
        QuestActionTrigger.INSTANCE.emit(actor, "v3_food_fishing_crate");
    }
}
