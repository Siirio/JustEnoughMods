package com.justenoughmods.achievementguide.integration.create;

import com.justenoughmods.achievementguide.advancement.QuestActionTrigger;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

/** Bounded proof attached to the actual supplied entity or one pending native input run. */
@Mod.EventBusSubscriber(modid = "jem_advancements")
public final class CreateRunProofs {
    private static final String ACTOR = "jemQuestCreateSupplierV1";
    private static final String RUN = "jemQuestCreateInputV1";
    private CreateRunProofs() {}

    @SubscribeEvent
    public static void supplied(ItemTossEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && !(player instanceof FakePlayer)
                && !player.isSpectator() && !player.getAbilities().instabuild) {
            CompoundTag proof = new CompoundTag();
            proof.putUUID("actor", player.getUUID());
            proof.put("input", event.getEntity().getItem().save(new CompoundTag()));
            event.getEntity().getPersistentData().put(ACTOR, proof);
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void connected(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (net.minecraftforge.fml.ModList.get().isLoaded("create")) CreateSteamPlacement.placed(event);
    }

    public static UUID actor(ItemEntity entity) {
        CompoundTag proof = entity.getPersistentData().getCompound(ACTOR);
        // Native entity merges or an intervening transformation remove personal proof.
        return proof.hasUUID("actor") && entity.getItem().save(new CompoundTag()).equals(proof.getCompound("input"))
                ? proof.getUUID("actor") : null;
    }

    public static void entitySuccess(ItemEntity input, UUID actor, String action) {
        if (input.level() instanceof ServerLevel level && actor != null) {
            input.getPersistentData().remove(ACTOR);
            emit(level, actor, action);
        }
    }

    public static boolean item(ItemStack stack, String id) {
        return !stack.isEmpty() && id.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }

    public static int count(IItemHandler inventory, String id) {
        int total = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (item(stack, id)) total += stack.getCount();
        }
        return total;
    }

    public static void bind(BlockEntity owner, IItemHandler input, UUID actor) {
        invalidate(owner);
        if (!(owner.getLevel() instanceof ServerLevel) || actor == null || input.getSlots() > 32) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor);
        run.put("input", snapshot(input));
        owner.getPersistentData().put(RUN, run);
        owner.setChanged();
    }

    public static UUID pending(BlockEntity owner, IItemHandler input) {
        CompoundTag run = owner.getPersistentData().getCompound(RUN);
        return run.hasUUID("actor") && input.getSlots() <= 32 && snapshot(input).equals(run.getList("input", 10))
                ? run.getUUID("actor") : null;
    }

    public static void invalidate(BlockEntity owner) {
        if (owner.getPersistentData().contains(RUN)) {
            owner.getPersistentData().remove(RUN);
            owner.setChanged();
        }
    }

    private static ListTag snapshot(IItemHandler input) {
        ListTag slots = new ListTag();
        for (int slot = 0; slot < input.getSlots(); slot++) slots.add(input.getStackInSlot(slot).save(new CompoundTag()));
        return slots;
    }

    public static void emit(ServerLevel level, UUID actor, String action) {
        if (actor == null) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(actor);
        if (player != null) QuestActionTrigger.INSTANCE.emit(player, action);
    }
}
