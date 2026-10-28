package com.justenoughmods.achievementguide.integration.create;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.items.IItemHandler;

/** One native restocker cycle on its actual gauge, with receipt on the actual receiving packager. */
public final class CreateGaugeProofs {
    private static final String KEY = "jemQuestGaugeV1_";
    private static final String RECEIVER = "jemQuestGaugeReceiverIdentityV1";
    private CreateGaugeProofs() {}
    private static String key(FactoryPanelBehaviour panel) { return KEY + panel.slot.name(); }
    private static FactoryPanelBlockEntity board(FactoryPanelBehaviour panel) { return (FactoryPanelBlockEntity)panel.blockEntity; }
    public static CompoundTag configuration(FactoryPanelBehaviour panel) {
        CompoundTag config = new CompoundTag();
        config.putString("filter", panel.getFilter().isEmpty() ? "" : CreateCargoTransfers.fingerprint(panel.getFilter()));
        config.putInt("amount", panel.getAmount()); config.putInt("row", panel.getValueSettings().row());
        config.putString("address", panel.recipeAddress); config.putUUID("network", panel.network);
        config.putBoolean("active", panel.active); config.putBoolean("restocker", board(panel).restocker);
        return config;
    }
    public static void configured(FactoryPanelBehaviour panel, ServerPlayer actor, CompoundTag before) {
        CompoundTag after = configuration(panel);
        if (before == null || before.equals(after) || actor instanceof FakePlayer || actor.isSpectator() || actor.getAbilities().instabuild) return;
        CompoundTag run = new CompoundTag();
        run.putUUID("actor", actor.getUUID()); run.putUUID("cycle", UUID.randomUUID()); run.put("configuration", after);
        panel.blockEntity.getPersistentData().put(key(panel), run); panel.blockEntity.setChanged();
    }
    private static int demand(FactoryPanelBehaviour panel) {
        return panel.getAmount() * (panel.getValueSettings().row() == 0 ? 1 : panel.getFilter().getMaxStackSize());
    }
    private static CompoundTag run(FactoryPanelBehaviour panel) {
        CompoundTag run = panel.blockEntity.getPersistentData().getCompound(key(panel));
        return run.hasUUID("actor") && run.hasUUID("cycle") && configuration(panel).equals(run.getCompound("configuration")) ? run : null;
    }
    public static boolean request(FactoryPanelBehaviour panel, PackageOrderWithCrafts order, Supplier<Boolean> nativeCall) {
        CompoundTag run = run(panel);
        if (!(panel.getWorld() instanceof ServerLevel level) || run == null || !board(panel).restocker || demand(panel) <= 0
                || panel.getFilter().isEmpty() || panel.recipeAddress.isBlank() || panel.getLevelInStorage() >= demand(panel)) return nativeCall.get();
        PackagerBlockEntity receiver = board(panel).getRestockedPackager();
        if (receiver == null) return nativeCall.get();
        ServerPlayer actor = level.getServer().getPlayerList().getPlayer(run.getUUID("actor"));
        if (actor == null) return nativeCall.get();
        if (!receiver.getPersistentData().hasUUID(RECEIVER)) {
            receiver.getPersistentData().putUUID(RECEIVER, UUID.randomUUID()); receiver.setChanged();
        }
        if (!run.contains("deficit")) {
            run.putInt("deficit", demand(panel) - panel.getLevelInStorage());
            run.putLong("receiver", receiver.getBlockPos().asLong());
            run.putUUID("receiver_identity", receiver.getPersistentData().getUUID(RECEIVER));
        }
        if (run.getLong("receiver") != receiver.getBlockPos().asLong()
                || !run.getUUID("receiver_identity").equals(receiver.getPersistentData().getUUID(RECEIVER))) return nativeCall.get();
        CompoundTag origin = new CompoundTag();
        origin.putLong("board", panel.getPos().asLong()); origin.putString("slot", panel.slot.name());
        origin.putUUID("cycle", run.getUUID("cycle")); origin.putString("dimension", level.dimension().location().toString());
        panel.blockEntity.setChanged();
        return CreateOrderProofs.request(actor, order, null, origin, nativeCall);
    }
    private static int count(IItemHandler inventory, ItemStack filter) {
        if (inventory.getSlots() < 0 || inventory.getSlots() > 4096) return -1;
        long count = 0;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack item = inventory.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(filter, item)) count += item.getCount();
        }
        return (int)Math.min(Integer.MAX_VALUE, count);
    }
    public static Receipt receiving(PackagerBlockEntity receiver, ItemStack box, boolean simulate) {
        CompoundTag proof = simulate ? null : CreateParcelProofs.proof(box);
        if (proof == null || !proof.hasUUID("parcel") || !(receiver.getLevel() instanceof ServerLevel level) || !proof.contains("gauge", 10)) return null;
        CompoundTag origin = proof.getCompound("gauge");
        BlockPos position = BlockPos.of(origin.getLong("board"));
        if (!origin.hasUUID("cycle") || !origin.getString("dimension").equals(level.dimension().location().toString())
                || !level.isLoaded(position) || !(level.getBlockEntity(position) instanceof FactoryPanelBlockEntity board)) return null;
        FactoryPanelBehaviour panel = board.panels.values().stream().filter(value -> value.slot.name().equals(origin.getString("slot"))).findFirst().orElse(null);
        CompoundTag run = panel == null ? null : run(panel);
        if (run == null || !run.getUUID("cycle").equals(origin.getUUID("cycle")) || !run.getUUID("actor").equals(proof.getUUID("actor"))
                || !run.hasUUID("receiver_identity") || !receiver.getPersistentData().hasUUID(RECEIVER)
                || !run.getUUID("receiver_identity").equals(receiver.getPersistentData().getUUID(RECEIVER))
                || run.getLong("receiver") != receiver.getBlockPos().asLong() || board.getRestockedPackager() != receiver) return null;
        IItemHandler inventory = receiver.targetInventory.getInventory();
        if (inventory == null || inventory.getSlots() < 0 || inventory.getSlots() > 4096) return null;
        ItemStack filter = panel.getFilter().copy();
        int before = count(inventory, filter);
        if (before < 0) return null;
        int payload = 0;
        var contents = com.simibubi.create.content.logistics.box.PackageItem.getContents(box);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack item = contents.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(filter, item)) payload += item.getCount();
        }
        return payload <= 0 ? null : new Receipt(panel, receiver, inventory, filter, proof.getUUID("parcel"), run.getUUID("cycle"), before, payload);
    }
    public static void received(Receipt receipt, boolean success) {
        if (!success || receipt == null || receipt.receiver.targetInventory.getInventory() != receipt.inventory) return;
        CompoundTag run = run(receipt.panel);
        if (run == null || !run.getUUID("cycle").equals(receipt.cycle)) return;
        int after = count(receipt.inventory, receipt.filter);
        if (after < 0 || after - receipt.before != receipt.payload) return;
        ListTag seen = run.getList("received", 8);
        String id = receipt.parcel.toString();
        if (seen.stream().anyMatch(value -> value.getAsString().equals(id)) || seen.size() >= 256) return;
        seen.add(StringTag.valueOf(id)); run.put("received", seen);
        run.putInt("delivered", (int)Math.min(Integer.MAX_VALUE, (long)run.getInt("delivered") + receipt.payload));
        if (after >= demand(receipt.panel) && run.getInt("delivered") >= run.getInt("deficit")) {
            CreateRunProofs.emit((ServerLevel)receipt.receiver.getLevel(), run.getUUID("actor"), "create_gauge_restock");
            receipt.panel.blockEntity.getPersistentData().remove(key(receipt.panel));
        }
        receipt.panel.blockEntity.setChanged();
    }
    public record Receipt(FactoryPanelBehaviour panel, PackagerBlockEntity receiver, IItemHandler inventory, ItemStack filter,
                          UUID parcel, UUID cycle, int before, int payload) {}
}
