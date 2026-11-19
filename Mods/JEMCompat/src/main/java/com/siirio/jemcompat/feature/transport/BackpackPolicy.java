package com.siirio.jemcompat.feature.transport;

import com.mojang.logging.LogUtils;
import com.siirio.jemrewards.PendingRewardContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.p3pp3rf1y.sophisticatedbackpacks.api.CapabilityBackpackWrapper;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackItem;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;
import net.p3pp3rf1y.sophisticatedcore.inventory.InventoryHandler;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;

public final class BackpackPolicy {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID BURDEN_MODIFIER_ID = UUID.fromString("af7253b1-5116-45a3-9c66-9686bf714e3e");
    private static final String JOURNAL_KEY = "JemTransportBackpackMigration";
    private static final String POLICY_VERSION_KEY = "JemTransportBackpackPolicyVersion";
    private static final int POLICY_VERSION = 1;
    private static final String LAST_OCCUPIED_KEY = "JemTransportLastOccupied";
    private static final String LAST_TOTAL_KEY = "JemTransportLastTotal";
    private static final String LAST_TENTHS_KEY = "JemTransportLastTenths";
    private static final double REDUCTION_PER_SLOT = 1.0 / 300.0;
    private static final double MAXIMUM_REDUCTION = 0.70;
    private static final int MAX_NESTING_DEPTH = 32;
    private static final Set<ResourceLocation> RETIRED_ITEMS = Set.of(
            id("inception_upgrade"),
            id("stack_upgrade_starter_tier"),
            id("stack_upgrade_tier_1"),
            id("stack_upgrade_tier_2"),
            id("stack_upgrade_tier_3"),
            id("stack_upgrade_tier_4"),
            id("stack_upgrade_omega_tier"),
            id("stack_downgrade_tier_1"),
            id("stack_downgrade_tier_2"),
            id("stack_downgrade_tier_3"),
            id("stack_upgrade_starter_tier_to_tier_1_conversion"),
            id("stack_upgrade_starter_tier_to_tier_2_conversion"),
            id("stack_upgrade_starter_tier_to_tier_3_conversion"),
            id("stack_upgrade_starter_tier_to_tier_4_conversion"),
            id("stack_upgrade_tier_1_to_tier_2_conversion"),
            id("stack_upgrade_tier_1_to_tier_3_conversion"),
            id("stack_upgrade_tier_1_to_tier_4_conversion"),
            id("stack_upgrade_tier_2_to_tier_3_conversion"),
            id("stack_upgrade_tier_2_to_tier_4_conversion"),
            id("stack_upgrade_tier_3_to_tier_4_conversion"));
    private BackpackPolicy() {
    }

    public static Set<ResourceLocation> retiredItems() {
        return RETIRED_ITEMS;
    }

    public static boolean isRetiredUpgrade(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return key != null && RETIRED_ITEMS.contains(key);
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    public static boolean isPortableContainer(ItemStack stack) {
        return isBackpack(stack) || stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()
                || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % 20 == 0) {
            forEachCarriedStack(player, stack -> migrate(stack, player));
        }
        if (player.tickCount % 5 == 0) {
            updateBurden(player);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            forEachCarriedStack(player, stack -> migrate(stack, player));
            player.getPersistentData().putInt(LAST_TOTAL_KEY, -1);
            updateBurden(player);
        }
    }

    public static void migrate(ItemStack stack, ServerPlayer owner) {
        if (!isBackpack(stack)) {
            return;
        }
        CompoundTag backpackTag = stack.getTag();
        if (backpackTag != null && backpackTag.getInt(POLICY_VERSION_KEY) >= POLICY_VERSION) {
            return;
        }
        IBackpackWrapper wrapper = stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(null);
        if (wrapper == null) {
            return;
        }
        try {
            CompoundTag journal = stack.getOrCreateTag().getCompound(JOURNAL_KEY);
            if (journal.isEmpty()) {
                MigrationPlan plan = createPlan(wrapper, 0);
                if (!plan.changed()) {
                    markMigrated(stack, wrapper);
                    return;
                }
                journal = plan.save("jem.transport.backpack_migration:" + UUID.randomUUID());
                stack.getOrCreateTag().put(JOURNAL_KEY, journal);
            }
            if (!journal.getBoolean("Applied")) {
                applySnapshot(wrapper, journal);
                journal.putBoolean("Applied", true);
                wrapper.markBackpackContentsChanged();
            }
            deliver(owner, stack, wrapper, journal);
        } catch (RuntimeException failure) {
            LOGGER.error("Backpack transport migration remains pending for {}", owner.getUUID(), failure);
        }
    }

    private static void deliver(ServerPlayer owner, ItemStack backpack, IBackpackWrapper wrapper, CompoundTag journal) {
        String source = journal.getString("Source");
        List<ItemStack> delivery = loadStacks(journal.getList("Delivery", Tag.TAG_COMPOUND));
        if (!PendingRewardContainer.hasIssued(owner.server, owner.getUUID(), source)) {
            PendingRewardContainer.enqueue(owner.server, owner.getUUID(), source,
                    "reward.jemcompat.backpack_migration", delivery);
        }
        if (PendingRewardContainer.hasIssued(owner.server, owner.getUUID(), source)) {
            backpack.getOrCreateTag().remove(JOURNAL_KEY);
            markMigrated(backpack, wrapper);
        }
    }

    private static MigrationPlan createPlan(IBackpackWrapper wrapper, int depth) {
        if (depth > MAX_NESTING_DEPTH) {
            throw new IllegalStateException("Backpack nesting exceeds the migration limit");
        }
        InventoryHandler inventory = wrapper.getInventoryHandler();
        UpgradeHandler upgrades = wrapper.getUpgradeHandler();
        List<ItemStack> finalInventory = new ArrayList<>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            finalInventory.add(ItemStack.EMPTY);
        }
        List<ItemStack> overflow = new ArrayList<>();
        List<ItemStack> delivery = new ArrayList<>();
        boolean changed = false;

        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty()) {
                continue;
            }
            if (isBackpack(current)) {
                ItemStack detached = current.copy();
                normalizeDetached(detached, delivery, depth + 1);
                delivery.add(detached);
                changed = true;
                continue;
            }
            int nativeLimit = Math.max(1, current.getItem().getMaxStackSize(current));
            int firstCount = Math.min(nativeLimit, current.getCount());
            finalInventory.set(slot, current.copyWithCount(firstCount));
            int remaining = current.getCount() - firstCount;
            while (remaining > 0) {
                int count = Math.min(nativeLimit, remaining);
                overflow.add(current.copyWithCount(count));
                remaining -= count;
                changed = true;
            }
        }
        for (int slot = 0; slot < finalInventory.size() && !overflow.isEmpty(); slot++) {
            if (!finalInventory.get(slot).isEmpty()) {
                continue;
            }
            finalInventory.set(slot, overflow.remove(0));
        }
        delivery.addAll(overflow);

        List<ItemStack> finalUpgrades = new ArrayList<>();
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            ItemStack current = upgrades.getStackInSlot(slot);
            if (isRetiredUpgrade(current)) {
                delivery.add(current.copy());
                finalUpgrades.add(ItemStack.EMPTY);
                changed = true;
            } else {
                finalUpgrades.add(current.copy());
            }
        }
        return new MigrationPlan(finalInventory, finalUpgrades, delivery, changed);
    }

    private static void normalizeDetached(ItemStack backpack, List<ItemStack> parentDelivery, int depth) {
        IBackpackWrapper wrapper = backpack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).orElse(null);
        if (wrapper == null) {
            throw new IllegalStateException("Nested backpack has no native wrapper");
        }
        MigrationPlan plan = createPlan(wrapper, depth);
        if (plan.changed()) {
            applyPlan(wrapper, plan);
            parentDelivery.addAll(plan.delivery());
        }
        markMigrated(backpack, wrapper);
    }

    private static void applyPlan(IBackpackWrapper wrapper, MigrationPlan plan) {
        InventoryHandler inventory = wrapper.getInventoryHandler();
        UpgradeHandler upgrades = wrapper.getUpgradeHandler();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            inventory.setStackInSlot(slot, plan.inventory().get(slot).copy());
        }
        for (int slot = 0; slot < upgrades.getSlots(); slot++) {
            upgrades.setStackInSlot(slot, plan.upgrades().get(slot).copy());
        }
        wrapper.refreshInventoryForUpgradeProcessing();
        wrapper.refreshInventoryForInputOutput();
        wrapper.markBackpackContentsChanged();
    }

    private static void applySnapshot(IBackpackWrapper wrapper, CompoundTag journal) {
        List<ItemStack> inventory = loadIndexed(journal.getList("Inventory", Tag.TAG_COMPOUND),
                wrapper.getInventoryHandler().getSlots());
        List<ItemStack> upgrades = loadIndexed(journal.getList("Upgrades", Tag.TAG_COMPOUND),
                wrapper.getUpgradeHandler().getSlots());
        applyPlan(wrapper, new MigrationPlan(inventory, upgrades, List.of(), true));
    }

    private static void markMigrated(ItemStack backpack, IBackpackWrapper wrapper) {
        backpack.getOrCreateTag().putInt(POLICY_VERSION_KEY, POLICY_VERSION);
        wrapper.markBackpackContentsChanged();
    }

    private static void updateBurden(ServerPlayer player) {
        int[] totals = {0, 0};
        forEachCarriedStack(player, stack -> {
            if (!isBackpack(stack)) {
                return;
            }
            stack.getCapability(CapabilityBackpackWrapper.getCapabilityInstance()).ifPresent(wrapper -> {
                InventoryHandler inventory = wrapper.getInventoryHandler();
                totals[1] += inventory.getSlots();
                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    if (!inventory.getStackInSlot(slot).isEmpty()) {
                        totals[0]++;
                    }
                }
            });
        });
        double reduction = Math.min(MAXIMUM_REDUCTION, totals[0] * REDUCTION_PER_SLOT);
        boolean excluded = player.isPassenger() || player.isFallFlying() || player.isInWaterOrBubble()
                || player.onClimbable() || player.getAbilities().flying;
        applyModifier(player, excluded ? 0.0 : reduction);
        int tenths = (int) Math.round(reduction * 1000.0);
        CompoundTag data = player.getPersistentData();
        if (data.getInt(LAST_OCCUPIED_KEY) != totals[0] || data.getInt(LAST_TOTAL_KEY) != totals[1]
                || data.getInt(LAST_TENTHS_KEY) != tenths) {
            data.putInt(LAST_OCCUPIED_KEY, totals[0]);
            data.putInt(LAST_TOTAL_KEY, totals[1]);
            data.putInt(LAST_TENTHS_KEY, tenths);
            TransportNetwork.sendBurden(player, totals[0], totals[1], reduction);
        }
    }

    private static void applyModifier(ServerPlayer player, double reduction) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        AttributeModifier current = speed.getModifier(BURDEN_MODIFIER_ID);
        if (current != null && Double.compare(current.getAmount(), -reduction) != 0) {
            speed.removeModifier(BURDEN_MODIFIER_ID);
            current = null;
        }
        if (reduction > 0.0 && current == null) {
            speed.addTransientModifier(new AttributeModifier(BURDEN_MODIFIER_ID, "JEM backpack burden",
                    -reduction, AttributeModifier.Operation.MULTIPLY_TOTAL));
        } else if (reduction == 0.0 && current != null) {
            speed.removeModifier(BURDEN_MODIFIER_ID);
        }
    }

    private static void forEachCarriedStack(ServerPlayer player, java.util.function.Consumer<ItemStack> consumer) {
        player.getInventory().items.forEach(consumer);
        player.getInventory().armor.forEach(consumer);
        player.getInventory().offhand.forEach(consumer);
        CuriosApi.getCuriosInventory(player).ifPresent(handler -> {
            var equipped = handler.getEquippedCurios();
            for (int slot = 0; slot < equipped.getSlots(); slot++) {
                consumer.accept(equipped.getStackInSlot(slot));
            }
        });
    }

    private static ListTag saveIndexed(List<ItemStack> stacks) {
        ListTag saved = new ListTag();
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = stack.save(new CompoundTag());
            entry.putInt("JemSlot", slot);
            saved.add(entry);
        }
        return saved;
    }

    private static ListTag saveStacks(List<ItemStack> stacks) {
        ListTag saved = new ListTag();
        stacks.stream().filter(stack -> !stack.isEmpty()).forEach(stack -> saved.add(stack.save(new CompoundTag())));
        return saved;
    }

    private static List<ItemStack> loadIndexed(ListTag saved, int size) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < size; slot++) {
            stacks.add(ItemStack.EMPTY);
        }
        for (Tag value : saved) {
            CompoundTag entry = (CompoundTag) value;
            int slot = entry.getInt("JemSlot");
            if (slot < 0 || slot >= size) {
                throw new IllegalStateException("Backpack migration slot is outside native capacity");
            }
            stacks.set(slot, ItemStack.of(entry));
        }
        return stacks;
    }

    private static List<ItemStack> loadStacks(ListTag saved) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Tag value : saved) {
            ItemStack stack = ItemStack.of((CompoundTag) value);
            if (!stack.isEmpty()) {
                stacks.add(stack);
            }
        }
        return stacks;
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("sophisticatedbackpacks", path);
    }

    private record MigrationPlan(List<ItemStack> inventory, List<ItemStack> upgrades,
                                 List<ItemStack> delivery, boolean changed) {
        private CompoundTag save(String source) {
            CompoundTag tag = new CompoundTag();
            tag.putString("Source", source);
            tag.put("Inventory", saveIndexed(inventory));
            tag.put("Upgrades", saveIndexed(upgrades));
            tag.put("Delivery", saveStacks(delivery));
            return tag;
        }
    }
}
